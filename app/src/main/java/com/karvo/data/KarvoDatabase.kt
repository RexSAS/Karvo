package com.karvo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Lesson::class, Event::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class KarvoDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun eventDao(): EventDao
}
