package com.example.shuffleit.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.shuffleit.data.database.Converters.IntListConverter
import com.example.shuffleit.data.database.Converters.UriConverter
import com.example.shuffleit.data.database.entities.PlayListEntity
import com.example.shuffleit.models.AudioFile

@Database(entities = [AudioFile::class, PlayListEntity::class], version = 1, exportSchema = false)
@TypeConverters(IntListConverter::class, UriConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioFileDao(): AudioFileDao
    abstract fun playListDao(): PlayListDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}