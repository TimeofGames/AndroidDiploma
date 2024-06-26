package com.example.shuffleit.models

import androidx.room.Embedded
import androidx.room.Relation
import com.example.shuffleit.data.database.entities.PlayListEntity

data class PlayListWithAudioFiles(
    @Embedded val playListEntity: PlayListEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val audioFiles: List<AudioFile>
)