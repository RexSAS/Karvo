package com.karvo.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After

class RoomLessonRepositoryTest : LessonRepositoryContract() {
    private var db: KarvoDatabase? = null

    override fun repo(): LessonRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, KarvoDatabase::class.java)
            .allowMainThreadQueries().build()
        db = database
        return RoomLessonRepository(database.lessonDao())
    }

    @After fun tearDown() { db?.close() }
}
