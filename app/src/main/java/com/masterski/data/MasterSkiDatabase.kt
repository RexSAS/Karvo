package com.masterski.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Lesson::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class MasterSkiDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
}
