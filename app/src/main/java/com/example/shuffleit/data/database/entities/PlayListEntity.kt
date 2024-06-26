package com.example.shuffleit.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlayListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val nowPlaying: Int,
    val queue: List<Long>,
    val path:String
)