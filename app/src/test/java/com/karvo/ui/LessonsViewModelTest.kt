package com.karvo.ui

import com.karvo.data.Lesson
import com.karvo.testing.FakeLessonRepository
import com.karvo.testing.MainDispatcherRule
import com.karvo.ui.lessons.LessonsViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LessonsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val repo = FakeLessonRepository()

    @Test fun groupsByMonthNewestFirst() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 9, 1), durationMinutes = 60, student = "A"))
        repo.save(Lesson(date = LocalDate.of(2026, 10, 5), durationMinutes = 60, student = "B"))
        val vm = LessonsViewModel(repo, com.karvo.testing.FakeDayNoteRepository(), com.karvo.testing.FakeSettingsRepository())
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9)), vm.groups.first { !it.isNullOrEmpty() }!!.map { it.month })
    }

    @Test fun loadingIsNullUntilFirstEmission() = runTest {
        val vm = LessonsViewModel(repo, com.karvo.testing.FakeDayNoteRepository(), com.karvo.testing.FakeSettingsRepository())
        assertNull(vm.groups.value)
        assertEquals(emptyList<Any>(), vm.groups.first { it != null })
    }

    @Test fun deleteRemovesLesson() = runTest {
        val id = repo.save(Lesson(date = LocalDate.of(2026, 10, 5), durationMinutes = 60, student = "B"))
        val vm = LessonsViewModel(repo, com.karvo.testing.FakeDayNoteRepository(), com.karvo.testing.FakeSettingsRepository())
        vm.delete(repo.get(id)!!); assertTrue(repo.allLessons().isEmpty())
    }

    @Test fun eachMonthCarriesItsTotal() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 10, 5), durationMinutes = 60, student = "A"))
        repo.save(Lesson(date = LocalDate.of(2026, 10, 9), durationMinutes = 90, student = "B"))
        repo.save(Lesson(date = LocalDate.of(2026, 9, 1), durationMinutes = 120, student = "C"))
        val groups = LessonsViewModel(repo, com.karvo.testing.FakeDayNoteRepository(), com.karvo.testing.FakeSettingsRepository()).groups.first { !it.isNullOrEmpty() }!!
        assertEquals(listOf(150L, 120L), groups.map { it.totalMinutes })
    }

    @Test fun upcomingNotesSkipThePastAndShowSoonestFirst() = runTest {
        val today = java.time.LocalDate.now(java.time.ZoneId.of(com.karvo.data.DEFAULT_TIME_ZONE))
        val notes = com.karvo.testing.FakeDayNoteRepository(mapOf(today.minusDays(1) to "old", today.plusDays(3) to "later", today to "now"))
        val vm = LessonsViewModel(FakeLessonRepository(), notes, com.karvo.testing.FakeSettingsRepository())
        val got = vm.notes.first { it.isNotEmpty() }
        assertEquals(listOf("now", "later"), got.map { it.second })
        vm.saveNote(today.plusDays(1), "  tomorrow ")
        assertEquals(listOf("now", "tomorrow", "later"), vm.notes.first { it.size == 3 }.map { it.second })
    }
}
