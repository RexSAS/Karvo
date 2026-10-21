package com.karvo.ui

import com.karvo.data.Lesson
import com.karvo.testing.FakeAuthenticator
import com.karvo.testing.FakeLessonRepository
import com.karvo.testing.FakeSettingsRepository
import com.karvo.testing.MainDispatcherRule
import com.karvo.ui.overview.OverviewViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        val vm = OverviewViewModel(repo, FakeSettingsRepository(1000)); vm.toggleReveal("Show earnings")
        assertTrue(vm.uiState.first { it.revealed }.revealed)
    }

    @Test fun hideAfterRevealHides() = runTest {
        val vm = OverviewViewModel(repo, FakeSettingsRepository(1000)); vm.toggleReveal("Show earnings")
        vm.uiState.first { it.revealed }
        vm.hide()
        assertFalse(vm.uiState.first { !it.revealed }.revealed)
    }

    @Test fun loadingUntilDataArrives() = runTest {
        val vm = OverviewViewModel(repo, FakeSettingsRepository(1000))
        assertTrue(vm.uiState.value.loading)
        assertFalse(vm.uiState.first { !it.loading }.loading)
    }

    private val clock = Clock.fixed(Instant.parse("2027-02-10T12:00:00Z"), ZoneOffset.UTC)

    @Test fun statsNullWithoutLessons() = runTest {
        val vm = OverviewViewModel(repo, FakeSettingsRepository(1000), clock)
        assertNull(vm.uiState.first { !it.loading }.stats)
    }

    @Test fun statsComputedForToday() = runTest {
        repo.save(Lesson(date = LocalDate.of(2027, 2, 1), durationMinutes = 60, student = "A"))
        repo.save(Lesson(date = LocalDate.of(2026, 12, 1), durationMinutes = 30, student = "B"))
        val stats = OverviewViewModel(repo, FakeSettingsRepository(2000), clock).uiState.first { it.stats != null }.stats!!
        assertEquals(1, stats.lessonsThisMonth)
        assertEquals(YearMonth.of(2027, 2), stats.lastSixMonths.last().month)
        assertEquals(2000L, stats.topStudents.first().earningsCents)
    }

    @Test fun todayUsesTimeZoneSetting() = runTest {
        val lateFeb = Clock.fixed(Instant.parse("2027-02-28T23:30:00Z"), ZoneOffset.UTC)
        repo.save(Lesson(date = LocalDate.of(2027, 3, 1), durationMinutes = 60, student = "A"))
        val vm = OverviewViewModel(repo, FakeSettingsRepository(null, zone = "Europe/Rome"), lateFeb)
        val stats = vm.uiState.first { it.stats != null }.stats!!
        assertEquals(1, stats.lessonsThisMonth)
        assertEquals(YearMonth.of(2027, 3), stats.lastSixMonths.last().month)
    }

    @Test fun statsDoNotChangeRevealDefault() = runTest {
        repo.save(Lesson(date = LocalDate.of(2027, 2, 1), durationMinutes = 60, student = "A"))
        val s = OverviewViewModel(repo, FakeSettingsRepository(1000), clock).uiState.first { it.stats != null }
        assertNotNull(s.stats); assertFalse(s.revealed)
    }

    @Test fun seasonsFollowStartMonthSetting() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 12, 1), durationMinutes = 60, student = "A"))
        val settings = FakeSettingsRepository(2000)
        val clock = Clock.fixed(Instant.parse("2027-01-10T12:00:00Z"), ZoneOffset.UTC)
        val vm = OverviewViewModel(repo, settings, clock)
        assertEquals("2026/27", vm.uiState.first { it.seasons != null }.seasons!!.current.season.label())
        settings.setSeasonStartMonth(1)
        assertEquals("2027", vm.uiState.first { it.seasons?.current?.season?.startMonth == 1 }.seasons!!.current.season.label())
    }

    @Test fun unpaidUsesZoneToday() = runTest {
        repo.save(Lesson(date = LocalDate.of(2027, 1, 1), durationMinutes = 45, student = "A"))
        repo.save(Lesson(date = LocalDate.of(2026, 12, 1), durationMinutes = 60, student = "B", paidOn = LocalDate.of(2026, 12, 20)))
        val clock = Clock.fixed(Instant.parse("2026-12-31T23:30:00Z"), ZoneOffset.UTC) // already 1 Jan in Rome
        val s = OverviewViewModel(repo, FakeSettingsRepository(2000, zone = "Europe/Rome"), clock).uiState.first { !it.loading }
        assertEquals(45L, s.unpaid.minutes)
        assertEquals(1, s.unpaid.months)
    }

    @Test fun noSeasonsWithoutLessons() = runTest {
        val s = OverviewViewModel(repo, FakeSettingsRepository(2000)).uiState.first { !it.loading }
        assertNull(s.seasons)
        assertEquals(0L, s.unpaid.minutes)
    }

    private suspend fun gatedVm(auth: FakeAuthenticator, settings: FakeSettingsRepository = FakeSettingsRepository(2000)): OverviewViewModel {
        repo.save(Lesson(date = LocalDate.of(2026, 10, 1), durationMinutes = 60, student = "A"))
        return OverviewViewModel(repo, settings, authenticator = auth).also { it.uiState.first { s -> !s.loading } }
    }

    @Test fun revealAsksWhenOnAndAvailable() = runTest {
        val auth = FakeAuthenticator()
        val vm = gatedVm(auth)
        vm.toggleReveal("Show earnings")
        assertTrue(vm.uiState.first { it.revealed }.revealed)
        assertEquals(1, auth.calls)
    }

    @Test fun failedAuthStaysHidden() = runTest {
        val auth = FakeAuthenticator(succeeds = false)
        val vm = gatedVm(auth)
        vm.toggleReveal("Show earnings")
        assertFalse(vm.uiState.value.revealed)
        assertEquals(1, auth.calls)
    }

    @Test fun hideNeverAsks() = runTest {
        val auth = FakeAuthenticator()
        val vm = gatedVm(auth)
        vm.toggleReveal("Show earnings"); vm.uiState.first { it.revealed }
        vm.toggleReveal("Show earnings")
        assertFalse(vm.uiState.first { !it.revealed }.revealed)
        assertEquals(1, auth.calls)
    }

    @Test fun unavailableRevealsWithoutPrompt() = runTest {
        val auth = FakeAuthenticator(available = false)
        val vm = gatedVm(auth)
        vm.toggleReveal("Show earnings")
        assertTrue(vm.uiState.first { it.revealed }.revealed)
        assertEquals(0, auth.calls)
    }

    @Test fun settingOffRevealsWithoutPrompt() = runTest {
        val auth = FakeAuthenticator()
        val settings = FakeSettingsRepository(2000).apply { setBiometricReveal(false) }
        val vm = gatedVm(auth, settings)
        vm.toggleReveal("Show earnings")
        assertTrue(vm.uiState.first { it.revealed }.revealed)
        assertEquals(0, auth.calls)
    }
}
