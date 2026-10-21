package com.karvo.ui

import com.karvo.data.Lesson
import com.karvo.testing.FakeLessonRepository
import com.karvo.testing.FakeSettingsRepository
import com.karvo.testing.MainDispatcherRule
import com.karvo.ui.overview.MonthDetailViewModel
import com.karvo.domain.PaidStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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

    private val dec = YearMonth.of(2026, 12)
    private fun vm(clock: Clock = Clock.systemUTC(), zone: String = "Europe/Rome") =
        MonthDetailViewModel(repo, FakeSettingsRepository(1000, zone = zone), dec, clock)

    @Test fun stateShowsPartlyPaidAndLatestDate() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 12, 1), durationMinutes = 60, student = "A", paidOn = LocalDate.of(2027, 1, 15)))
        repo.save(Lesson(date = LocalDate.of(2026, 12, 2), durationMinutes = 60, student = "B"))
        val s = vm().uiState.first { it.lessons.size == 2 }
        assertEquals(PaidStatus.PARTLY_PAID, s.paidStatus)
        assertEquals(LocalDate.of(2027, 1, 15), s.paidOn)
    }

    @Test fun markPaidKeepsEarlierDates() = runTest {
        val early = LocalDate.of(2026, 12, 20)
        val a = repo.save(Lesson(date = LocalDate.of(2026, 12, 1), durationMinutes = 60, student = "A", paidOn = early))
        val b = repo.save(Lesson(date = LocalDate.of(2026, 12, 2), durationMinutes = 60, student = "B"))
        val vm = vm()
        vm.markPaid(LocalDate.of(2027, 1, 15))
        assertEquals(PaidStatus.PAID, vm.uiState.first { it.paidStatus == PaidStatus.PAID }.paidStatus)
        assertEquals(early, repo.get(a)!!.paidOn)
        assertEquals(LocalDate.of(2027, 1, 15), repo.get(b)!!.paidOn)
    }

    @Test fun markUnpaidClearsMonth() = runTest {
        val a = repo.save(Lesson(date = LocalDate.of(2026, 12, 1), durationMinutes = 60, student = "A", paidOn = LocalDate.of(2027, 1, 15)))
        val other = repo.save(Lesson(date = LocalDate.of(2027, 1, 1), durationMinutes = 60, student = "C", paidOn = LocalDate.of(2027, 2, 1)))
        vm().markUnpaid()
        assertEquals(null, repo.get(a)!!.paidOn)
        assertEquals(LocalDate.of(2027, 2, 1), repo.get(other)!!.paidOn)
    }

    @Test fun todayUsesConfiguredZone() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 12, 1), durationMinutes = 60, student = "A"))
        val clock = Clock.fixed(Instant.parse("2026-12-31T23:30:00Z"), ZoneOffset.UTC)
        val s = vm(clock).uiState.first { it.lessons.isNotEmpty() }
        assertEquals(LocalDate.of(2027, 1, 1), s.today)
    }
}
