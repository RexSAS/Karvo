package com.karvo.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LessonDaoTest {
    private lateinit var db: KarvoDatabase
    private lateinit var dao: LessonDao

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KarvoDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.lessonDao()
    }

    @After fun tearDown() = db.close()

    @Test fun orderedNewestFirst() = runTest {
        dao.insert(Lesson(date = LocalDate.of(2026, 1, 1), durationMinutes = 60, student = "A"))
        dao.insert(Lesson(date = LocalDate.of(2026, 3, 1), durationMinutes = 60, student = "B"))
        assertEquals(listOf("B", "A"), dao.observeAll().first().map { it.student })
    }

    @Test fun studentNamesDistinctMostRecentFirst() = runTest {
        dao.insert(Lesson(date = LocalDate.of(2026, 1, 1), durationMinutes = 60, student = "Anna"))
        dao.insert(Lesson(date = LocalDate.of(2026, 2, 1), durationMinutes = 60, student = "Bob"))
        dao.insert(Lesson(date = LocalDate.of(2026, 3, 1), durationMinutes = 60, student = "Anna"))
        assertEquals(listOf("Anna", "Bob"), dao.observeStudentNames().first())
    }

    @Test fun dateRoundTrips() = runTest {
        val id = dao.insert(Lesson(date = LocalDate.of(2026, 10, 20), durationMinutes = 45, student = "A"))
        assertEquals(LocalDate.of(2026, 10, 20), dao.getById(id)!!.date)
    }

    @Test fun replaceAllRollsBackWhenInsertFails() = runTest {
        dao.insert(Lesson(date = LocalDate.of(2026, 1, 1), durationMinutes = 60, student = "Keep1"))
        dao.insert(Lesson(date = LocalDate.of(2026, 1, 2), durationMinutes = 60, student = "Keep2"))
        val clash = Lesson(id = 99, date = LocalDate.of(2026, 2, 1), durationMinutes = 60, student = "Dup")
        val failed = runCatching { dao.replaceAll(listOf(clash, clash.copy(student = "Dup2"))) }.isFailure
        assertEquals(true, failed)
        assertEquals(listOf("Keep2", "Keep1"), dao.getAll().map { it.student })
    }
}
