package com.example.shuffleit.data.database.Converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IntListConverter {
    @TypeConverter
    fun fromIntList(value: List<Long>): String {
        val gson = Gson()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toIntList(value: String): List<Long> {
        val gson = Gson()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type)
    }
}