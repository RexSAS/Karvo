package com.masterski.ui

import com.masterski.data.Lesson
import com.masterski.testing.FakeLessonRepository
import com.masterski.testing.FakeSettingsRepository
import com.masterski.testing.MainDispatcherRule
import com.masterski.ui.settings.SettingsViewModel
import com.masterski.ui.settings.availableTimeZones
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val repo = FakeLessonRepository()

    @Test fun showsStoredRate() = runTest {
        val vm = SettingsViewModel(FakeSettingsRepository(2500), repo)
        assertEquals("25.00", vm.uiState.first { it.rateText.isNotEmpty() }.rateText)
    }

    @Test fun commaBecomesDotWhileTyping() = runTest {
        val vm = SettingsViewModel(FakeSettingsRepository(), repo); vm.onRateChange("25,5")
        assertEquals("25.5", vm.uiState.value.rateText)
    }

    @Test fun savesValidRateAndReformats() = runTest {
        val s = FakeSettingsRepository(null); val vm = SettingsViewModel(s, repo)
        vm.onRateChange("25,5"); vm.saveRate()
        assertEquals(2550L, s.hourlyRateCents.first()); assertFalse(vm.uiState.value.rateError)
        assertEquals("25.50", vm.uiState.value.rateText)
    }

    @Test fun timeZoneChange() = runTest {
        val s = FakeSettingsRepository(); val vm = SettingsViewModel(s, repo)
        assertEquals("Europe/Rome", vm.uiState.first().timeZoneId)
        vm.onTimeZoneSelected("Europe/Zurich"); assertEquals("Europe/Zurich", s.timeZoneId.first())
    }

    @Test fun unknownTimeZoneIgnored() = runTest {
        val s = FakeSettingsRepository(); val vm = SettingsViewModel(s, repo)
        vm.onTimeZoneSelected("Nowhere/Land"); assertEquals("Europe/Rome", s.timeZoneId.first())
    }

    @Test fun exportFileNameUsesZone() = runTest {
        val clock = Clock.fixed(Instant.parse("2026-10-19T22:30:00Z"), ZoneOffset.UTC)
        assertEquals("masterski-lessons-2026-10-20.csv", SettingsViewModel(FakeSettingsRepository(), repo, clock).exportFileName())
    }

    @Test fun zoneListHasRome() = assertTrue("Europe/Rome" in availableTimeZones)

    @Test fun invalidRateShowsErrorAndKeepsOld() = runTest {
        val s = FakeSettingsRepository(1000); val vm = SettingsViewModel(s, repo)
        vm.onRateChange("abc"); vm.saveRate()
        assertTrue(vm.uiState.value.rateError); assertEquals(1000L, s.hourlyRateCents.first())
    }

    @Test fun blankClearsRate() = runTest {
        val s = FakeSettingsRepository(1000); val vm = SettingsViewModel(s, repo)
        vm.onRateChange(""); vm.saveRate(); assertNull(s.hourlyRateCents.first())
    }

    @Test fun csvContainsLessons() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 10, 20), durationMinutes = 90, student = "Anna"))
        assertTrue(SettingsViewModel(FakeSettingsRepository(), repo).buildCsv().contains("2026-10-20,Anna,90,1.5,"))
    }

    @Test fun saveRateTwiceIsIdempotent() = runTest {
        val s = FakeSettingsRepository(null); val vm = SettingsViewModel(s, repo)
        vm.onRateChange("30"); vm.saveRate(); vm.saveRate()
        assertEquals(3000L, s.hourlyRateCents.first()); assertEquals(1, s.rateWrites)
    }

    @Test fun saveRateWithoutEditDoesNotWrite() = runTest {
        val s = FakeSettingsRepository(2500); val vm = SettingsViewModel(s, repo)
        vm.saveRate()
        assertEquals(2500L, s.hourlyRateCents.first()); assertEquals(0, s.rateWrites)
    }

    @Test fun typedRateSavedWithoutDone() = runTest {
        val s = FakeSettingsRepository(null); val vm = SettingsViewModel(s, repo)
        vm.onRateChange("30")
        vm.saveRate() // what the screen's onDispose / ON_STOP path calls
        assertEquals(3000L, s.hourlyRateCents.first())
    }
}
