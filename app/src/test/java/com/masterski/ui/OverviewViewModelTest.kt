package com.masterski.ui

import com.masterski.data.Lesson
import com.masterski.testing.FakeLessonRepository
import com.masterski.testing.FakeSettingsRepository
import com.masterski.testing.MainDispatcherRule
import com.masterski.ui.overview.OverviewViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OverviewViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val repo = FakeLessonRepository()

    @Test fun totalsWithRate() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 10, 1), durationMinutes = 90, student = "A"))
        repo.save(Lesson(date = LocalDate.of(2026, 9, 1), durationMinutes = 60, student = "B"))
        val vm = OverviewViewModel(repo, FakeSettingsRepository(2000))
        val s = vm.uiState.first { it.months.size == 2 }
        assertEquals(150, s.totalMinutes); assertEquals(5000L, s.totalEarningsCents)
        assertEquals(3000L, s.months[0].earningsCents); assertTrue(s.rateSet); assertFalse(s.revealed)
    }

    @Test fun noRateMeansNullEarnings() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 10, 1), durationMinutes = 60, student = "A"))
        val s = OverviewViewModel(repo, FakeSettingsRepository(null)).uiState.first { it.months.isNotEmpty() }
        assertNull(s.totalEarningsCents); assertNull(s.months[0].earningsCents); assertFalse(s.rateSet)
    }

    @Test fun rateChangeRecalculatesPastMonths() = runTest {
        val settings = FakeSettingsRepository(1000)
        repo.save(Lesson(date = LocalDate.of(2025, 1, 1), durationMinutes = 60, student = "A"))
        val vm = OverviewViewModel(repo, settings); vm.uiState.first { it.months.isNotEmpty() }
        settings.setHourlyRateCents(3000)
        assertEquals(3000L, vm.uiState.first { it.totalEarningsCents == 3000L }.months[0].earningsCents)
    }

    @Test fun toggleReveal() = runTest {
        val vm = OverviewViewModel(repo, FakeSettingsRepository(1000)); vm.toggleReveal()
        assertTrue(vm.uiState.first { it.revealed }.revealed)
    }

    @Test fun hideAfterRevealHides() = runTest {
        val vm = OverviewViewModel(repo, FakeSettingsRepository(1000)); vm.toggleReveal()
        vm.uiState.first { it.revealed }
        vm.hide()
        assertFalse(vm.uiState.first { !it.revealed }.revealed)
    }

    @Test fun loadingUntilDataArrives() = runTest {
        val vm = OverviewViewModel(repo, FakeSettingsRepository(1000))
        assertTrue(vm.uiState.value.loading)
        assertFalse(vm.uiState.first { !it.loading }.loading)
    }
}
