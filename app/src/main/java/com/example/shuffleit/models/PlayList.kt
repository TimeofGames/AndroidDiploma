package com.example.shuffleit.models

data class PlayList(val id:Long, val title:String, var playlist:List<AudioFile>, private var nowPlaying:Int, private var queue:List<Long>, private var path:String) {

    fun getAudio():AudioFile{
        return getAudioFromQueue()
    }

    fun getPath():String{
        return path
    }

    private fun getAudioFromQueue():AudioFile{
        return playlist.filter { queue[nowPlaying] == it.id }.first()
    }

    fun getNowPlaying():Int{
        return nowPlaying
    }

    fun setNowPlaying(nowPlaying: Int){
        this.nowPlaying = nowPlaying
    }

    fun getQueue():List<Long>{
        return queue
    }

    fun getNextAudio():AudioFile{
        if(nowPlaying+1 == playlist.size){
            reloadQueue()
            nowPlaying = 0
            return getAudioFromQueue()
        }
        nowPlaying++
        return getAudioFromQueue()
    }

    fun getPreviousAudio():AudioFile{
        if(nowPlaying == 0){
            reloadQueue()
            nowPlaying = 0
            return getAudioFromQueue()
        }
        nowPlaying--
        return getAudioFromQueue()
    }

    private fun reloadQueue(){
        queue = queue.shuffled()
    }
}