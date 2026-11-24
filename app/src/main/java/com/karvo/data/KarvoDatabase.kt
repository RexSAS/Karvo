package com.karvo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Lesson::class, Event::class, Rate::class], version = 5, exportSchema = true)
@TypeConverters(Converters::class)
abstract class KarvoDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun eventDao(): EventDao
    abstract fun rateDao(): RateDao
}
