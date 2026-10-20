package com.masterski.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LessonRepositoryTest {
    private lateinit var db: MasterSkiDatabase
    private lateinit var repo: RoomLessonRepository
    private val d = LocalDate.of(2026, 10, 20)

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MasterSkiDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = RoomLessonRepository(db.lessonDao())
    }

    @After fun tearDown() = db.close()

    @Test fun editUpdatesNotDuplicates() = runTest {
        val id = repo.save(Lesson(date = d, durationMinutes = 60, student = "Anna"))
        repo.save(repo.get(id)!!.copy(durationMinutes = 90))
        val all = repo.allLessons(); assertEquals(1, all.size); assertEquals(90, all[0].durationMinutes)
    }

    @Test fun restoreKeepsId() = runTest {
        val id = repo.save(Lesson(date = d, durationMinutes = 60, student = "Anna"))
        val l = repo.get(id)!!; repo.delete(l); repo.restore(l)
        assertEquals(l, repo.get(id))
    }

    @Test fun normalizes() = runTest {
        val id = repo.save(Lesson(date = d, durationMinutes = 60, student = "  Anna ", notes = "   "))
        val l = repo.get(id)!!; assertEquals("Anna", l.student); assertNull(l.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroDuration() = runTest { repo.save(Lesson(date = d, durationMinutes = 0, student = "A")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankStudent() = runTest { repo.save(Lesson(date = d, durationMinutes = 60, student = "  ")) }
}
