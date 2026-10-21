package com.karvo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Lesson::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class KarvoDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
}
