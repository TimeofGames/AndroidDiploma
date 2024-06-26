package com.example.shuffleit.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.shuffleit.data.database.entities.PlayListEntity
import com.example.shuffleit.models.PlayListWithAudioFiles

@Dao
interface PlayListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playList: PlayListEntity): Long

    @Delete
    suspend fun delete(playList: PlayListEntity)
    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlayList(id: Long): PlayListEntity?
    @Transaction
    @Query("SELECT * FROM playlists")
    suspend fun getAllPlayLists(): List<PlayListEntity>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlayListWithAudioFiles(id: Long): PlayListWithAudioFiles?

    @Query("UPDATE playlists SET nowPlaying = :nowPlaying WHERE id = :id")
    suspend fun updateNowPlaying(id: Long, nowPlaying: Int)

    @Query("UPDATE playlists SET queue = :queue WHERE id = :id")
    suspend fun updateQueue(id: Long, queue: List<Long>)
}