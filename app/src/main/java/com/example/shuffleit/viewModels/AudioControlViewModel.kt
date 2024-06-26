package com.example.shuffleit.viewModels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.shuffleit.models.PlayList
import com.example.shuffleit.Utils
import android.widget.SeekBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.shuffleit.Utils.Companion.getAudioFilesFromUri
import com.example.shuffleit.data.database.AppDatabase
import com.example.shuffleit.data.database.entities.PlayListEntity
import com.example.shuffleit.models.AudioFile
import com.google.gson.Gson
import com.mikepenz.fastadapter.dsl.genericFastAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException

class AudioControlViewModel(private val application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application.applicationContext)
    private val playListDao = database.playListDao()
    private val audioFileDao = database.audioFileDao()

    private var audioManager: AudioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var _playlists = MutableLiveData(getAllPlayLists())
    val playlists: LiveData<ArrayList<PlayListEntity>> get() = _playlists

    private val _isPlaying = MutableLiveData<Boolean>().apply { value = false }
    val isPlaying: MutableLiveData<Boolean> get() = _isPlaying

    private lateinit var playlist: PlayList
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    private val _currentTime = MutableLiveData<Int>()
    val currentTime: LiveData<Int> get() = _currentTime
    val currentTimeString = _currentTime.map { Utils.intToTime(it) }

    private val _trackLen = MutableLiveData<Int>()
    val trackLen: LiveData<Int> get() = _trackLen
    val trackLenString = _trackLen.map { Utils.intToTime(it) }

    private val _trackTitle = MutableLiveData<String>()
    val trackTitle: LiveData<String> get() = _trackTitle

    private val _trackImage = MutableLiveData<Bitmap?>()
    val trackImage: LiveData<Bitmap?> get() = _trackImage

    private val sharedPreferences = application.getSharedPreferences("LastPlayedPlayList", Context.MODE_PRIVATE)

    val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                seekTo(progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            startChangeSeekBar()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            stopChangeSeekBar()
            seekTo(seekBar.progress)
        }
    }

    init {
        _trackLen.value = 0
        _currentTime.value = 0
        _playlists.value?.addAll(getAllPlayLists())
        val playlistId = sharedPreferences.getLong("PlaylistId", -1)
        val nowPlaying = sharedPreferences.getInt("nowPlaying", -1)
        val currentTime = sharedPreferences.getInt("currentTime",0)
        if (playlistId.toInt() != -1) {
            viewModelScope.launch {
                val retrievedPlaylist = getPlayList(application.applicationContext, playlistId)
                retrievedPlaylist?.let {
                    playlist = it
                    if (nowPlaying != -1) {
                        playlist.setNowPlaying(nowPlaying)
                        initMediaPlayer(playlist.getAudio().path)
                        if(currentTime != 0){
                            mediaPlayer?.seekTo(currentTime)
                            updateCurrentTime()
                        }
                    }
                }
            }
        }
    }

    fun onPlaylistClick(playlist: PlayListEntity) {
        selectPlaylist(playlist)
    }

    fun initMediaPlayer(audioFile: Uri) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(application.applicationContext, audioFile)
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            prepare()
            setOnPreparedListener {
                _trackLen.value = it.duration / 1000
            }
            setOnCompletionListener {
                nextTrack()
            }
        }
        retrieveMetadata(audioFile)
    }

    private fun retrieveMetadata(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(getApplication<Application>().applicationContext, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val albumArt = retriever.embeddedPicture
            retriever.release()

            withContext(Dispatchers.Main) {
                _trackTitle.value = title ?: "Unknown"
                _trackImage.value = albumArt?.let{ BitmapFactory.decodeByteArray(it, 0, it.size) }
            }
        }
    }

    fun selectPlaylist(playListEntity: PlayListEntity){
        if(::playlist.isInitialized) {
            updateQueue(playlist.id, playlist.getQueue())
        }
        viewModelScope.launch {
            playlist = getPlayList(application.applicationContext,playListEntity.id)!!
            initMediaPlayer(playlist.getAudio().path)
        }
    }

    fun playPause() {
        if (_isPlaying.value == true) {
            stop()
        } else {
            play()
        }
    }

    private fun play() {
        updateCurrentTime()
        mediaPlayer?.start()
        _isPlaying.value = true
    }

    private fun stop() {
        handler.removeCallbacks(updateCurrentTimeRunnable)
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    fun volumeUp() {
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
    }

    fun volumeDown() {
        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
    }

    fun nextTrack(){
        initMediaPlayer(playlist.getNextAudio().path)
        play()
    }

    fun previousTrack(){
        initMediaPlayer(playlist.getPreviousAudio().path)
        play()
    }

    fun seekTo(seekTo: Int) {
        mediaPlayer?.seekTo(seekTo * 1000)
        updateCurrentTime()
    }

    fun startChangeSeekBar() {
        handler.removeCallbacks(updateCurrentTimeRunnable)
    }

    fun stopChangeSeekBar() {
        handler.removeCallbacks(updateCurrentTimeRunnable)
    }

    private fun updateCurrentTime() {
        _currentTime.value = (mediaPlayer?.currentPosition ?: 0) / 1000
        handler.postDelayed(updateCurrentTimeRunnable, 1000)
    }

    private val updateCurrentTimeRunnable = object : Runnable {
        override fun run() {
            updateCurrentTime()
        }
    }

    fun addPlaylist(uri: Uri, name: String) {
        viewModelScope.launch {
            val newPlaylistEntity = PlayListEntity(
                title = name,
                nowPlaying = 0,
                queue = emptyList(),
                path = uri.toString()
            )

            val playlistId = playListDao.insert(newPlaylistEntity)
            val audioFiles = Utils.getAudioFilesFromUri(application.applicationContext, uri, playlistId)
            val audioFileIds:ArrayList<Long> = arrayListOf()
            audioFiles.forEach { audioFile ->
                audioFileIds.add(audioFileDao.insert(audioFile))
            }
            val queue = shuffle(audioFileIds)
            updateQueue(playlistId, queue)
            _playlists.value?.add(playListDao.getPlayList(playlistId)!!)
            _playlists.value = _playlists.value
        }
    }

    private fun shuffle(audioFileIds : List<Long>): List<Long>{
        val queue = audioFileIds.shuffled()
        return queue
    }

    fun insertPlayList(playList: PlayList) {
        val playListEntity = PlayListEntity(
            title = playList.title,
            nowPlaying = playList.getNowPlaying(),
            queue = playList.getQueue(),
            path = playList.getPath()
        )
        viewModelScope.launch {
            playListDao.insert(playListEntity)
            playList.playlist.forEach { audioFile ->    
                audioFileDao.insert(AudioFile(path = audioFile.path, playlistId = playListEntity.id))
            }
        }
    }

    fun deletePlayList(playListEntity: PlayListEntity) {
        viewModelScope.launch {
            playListDao.delete(playListEntity)

            if(::playlist.isInitialized && playlist.id == playListEntity.id){
                mediaPlayer?.release()
                mediaPlayer = null
                _trackTitle.value = ""
                _trackImage.value = null
                _trackLen.value = 0
            }
            val updatedPlaylists = withContext(Dispatchers.IO) {
                playListDao.getAllPlayLists()
            }
            _playlists.value = updatedPlaylists as ArrayList<PlayListEntity>
        }
    }

    fun getAllPlayLists(): ArrayList<PlayListEntity> {
        var playLists = arrayListOf<PlayListEntity>()
        viewModelScope.launch {
            playLists.addAll(playListDao.getAllPlayLists())
        }
        return playLists
    }

    suspend fun getPlayList(context: Context, id: Long): PlayList? {
        val playListWithAudioFiles = playListDao.getPlayListWithAudioFiles(id)
        return playListWithAudioFiles?.let {
            val audioFilesFromDb = it.audioFiles
            val playlistEntity = it.playListEntity
            val uri = Uri.parse(playlistEntity.path)

            val audioFilesFromUri = getAudioFilesFromUri(context, uri, id)

            val updatedPlaylist = mutableListOf<AudioFile>()
            val updatedQueue = mutableListOf<Long>()

            // Проверяем существующие файлы
            val existingFiles = audioFilesFromDb.filter { dbFile ->
                audioFilesFromUri.any { uriFile -> uriFile.path == dbFile.path }
            }

            updatedPlaylist.addAll(existingFiles)
            updatedQueue.addAll(existingFiles.map { it.id })

            // Проверяем новые файлы
            val newFiles = audioFilesFromUri.filter { uriFile ->
                audioFilesFromDb.none { dbFile -> dbFile.path == uriFile.path }
            }

            newFiles.forEach { newFile ->
                val newId = audioFileDao.insert(newFile) // Генерация ID для новых файлов
                newFile.id = newId
            }

            updatedPlaylist.addAll(newFiles)
            updatedQueue.addAll(newFiles.map { it.id }.shuffled()) // Перемешиваем новые файлы и добавляем в очередь

            PlayList(
                playlistEntity.id,
                playlistEntity.title,
                updatedPlaylist,
                playlistEntity.nowPlaying,
                updatedQueue,
                playlistEntity.path
            )
        }
    }

    fun updateNowPlaying(id: Long, nowPlaying: Int) {
        viewModelScope.launch {
            playListDao.updateNowPlaying(id, nowPlaying)
        }
    }

    fun updateQueue(id: Long, queue: List<Long>) {
        viewModelScope.launch {
            playListDao.updateQueue(id, queue)
        }
    }



    fun getAudioFile(id: Long): LiveData<AudioFile?> {
        val audioFile = MutableLiveData<AudioFile?>()
        viewModelScope.launch {
            audioFile.postValue(audioFileDao.getAudioFile(id))
        }
        return audioFile
    }


    fun onStop(){
        val editor = sharedPreferences.edit()
        if(::playlist.isInitialized) {
            editor.putLong("PlaylistId", playlist.id)
            editor.apply()
            editor.putInt("nowPlaying", playlist.getNowPlaying())
            editor.apply()
            editor.putInt("currentTime", mediaPlayer!!.currentPosition)
            editor.apply()
            updateQueue(playlist.id, playlist.getQueue())
        }
        mediaPlayer?.release()
        handler.removeCallbacks(updateCurrentTimeRunnable)
    }
}
