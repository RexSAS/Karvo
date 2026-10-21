package com.karvo.ui

import com.karvo.data.Lesson
import com.karvo.testing.FakeLessonRepository
import com.karvo.testing.FakeSettingsRepository
import com.karvo.testing.MainDispatcherRule
import com.karvo.ui.overview.MonthDetailViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MonthDetailViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val repo = FakeLessonRepository()

    @Test fun onlyThatMonth() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 10, 1), durationMinutes = 60, student = "A"))
        repo.save(Lesson(date = LocalDate.of(2025, 10, 1), durationMinutes = 60, student = "B"))
        val s = MonthDetailViewModel(repo, FakeSettingsRepository(1000), YearMonth.of(2026, 10)).uiState.first { it.lessons.isNotEmpty() }
        assertEquals(listOf("A"), s.lessons.map { it.student }); assertEquals(1000L, s.earningsCents)
    }
}
