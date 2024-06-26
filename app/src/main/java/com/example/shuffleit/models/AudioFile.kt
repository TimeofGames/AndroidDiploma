package com.example.shuffleit.models

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.shuffleit.data.database.entities.PlayListEntity

@Entity(
    tableName = "audio_files",
    foreignKeys = [ForeignKey(
        entity = PlayListEntity::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["playlistId"])]
)
data class AudioFile(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "path") val path: Uri,
    @ColumnInfo(name = "playlistId") val playlistId: Long
)
