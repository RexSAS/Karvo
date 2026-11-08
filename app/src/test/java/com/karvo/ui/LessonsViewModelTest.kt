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
        val vm = LessonsViewModel(repo, com.karvo.testing.FakeEventRepository(), com.karvo.testing.FakeSettingsRepository())
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9)), vm.groups.first { !it.isNullOrEmpty() }!!.map { it.month })
    }

    @Test fun loadingIsNullUntilFirstEmission() = runTest {
        val vm = LessonsViewModel(repo, com.karvo.testing.FakeEventRepository(), com.karvo.testing.FakeSettingsRepository())
        assertNull(vm.groups.value)
        assertEquals(emptyList<Any>(), vm.groups.first { it != null })
    }

    @Test fun deleteRemovesLesson() = runTest {
        val id = repo.save(Lesson(date = LocalDate.of(2026, 10, 5), durationMinutes = 60, student = "B"))
        val vm = LessonsViewModel(repo, com.karvo.testing.FakeEventRepository(), com.karvo.testing.FakeSettingsRepository())
        vm.delete(repo.get(id)!!); assertTrue(repo.allLessons().isEmpty())
    }

    @Test fun eachMonthCarriesItsTotal() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 10, 5), durationMinutes = 60, student = "A"))
        repo.save(Lesson(date = LocalDate.of(2026, 10, 9), durationMinutes = 90, student = "B"))
        repo.save(Lesson(date = LocalDate.of(2026, 9, 1), durationMinutes = 120, student = "C"))
        val groups = LessonsViewModel(repo, com.karvo.testing.FakeEventRepository(), com.karvo.testing.FakeSettingsRepository()).groups.first { !it.isNullOrEmpty() }!!
        assertEquals(listOf(150L, 120L), groups.map { it.totalMinutes })
    }

    @Test fun upcomingEventsAreTheNextThreeDays() = runTest {
        val today = java.time.LocalDate.now(java.time.ZoneId.of(com.karvo.data.DEFAULT_TIME_ZONE))
        val ev = com.karvo.testing.FakeEventRepository(listOf(
            com.karvo.data.Event(1, today.minusDays(1), "old"), com.karvo.data.Event(2, today, "now"), com.karvo.data.Event(3, today, "also now"),
            com.karvo.data.Event(4, today.plusDays(3), "in three"), com.karvo.data.Event(5, today.plusDays(4), "too far")))
        val vm = LessonsViewModel(FakeLessonRepository(), ev, com.karvo.testing.FakeSettingsRepository())
        val got = vm.upcoming.first { it.isNotEmpty() }
        assertEquals(listOf(0 to listOf("now", "also now"), 3 to listOf("in three")), got.map { it.second to it.third.map { e -> e.text } })
    }
}
