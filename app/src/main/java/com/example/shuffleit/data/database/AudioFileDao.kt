package com.example.shuffleit.data.database

import androidx.room.*
import com.example.shuffleit.data.database.entities.PlayListEntity
import com.example.shuffleit.models.AudioFile
import com.example.shuffleit.models.PlayListWithAudioFiles

@Dao
interface AudioFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audioFile: AudioFile): Long

    @Delete
    suspend fun delete(audioFile: AudioFile)

    @Query("SELECT * FROM audio_files WHERE id = :id")
    suspend fun getAudioFile(id: Long): AudioFile?
}

