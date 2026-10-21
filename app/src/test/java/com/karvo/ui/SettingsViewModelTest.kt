package com.karvo.ui

import com.karvo.data.LanguageChoice
import com.karvo.data.Lesson
import com.karvo.data.ThemeChoice
import com.karvo.testing.FakeAuthenticator
import com.karvo.testing.FakeLanguageController
import com.karvo.testing.FakeLessonRepository
import com.karvo.testing.FakeSettingsRepository
import com.karvo.testing.MainDispatcherRule
import com.karvo.ui.settings.SettingsViewModel
import com.karvo.ui.settings.availableTimeZones
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
        val vm = SettingsViewModel(FakeSettingsRepository(2500), repo, FakeLanguageController())
        assertEquals("25.00", vm.uiState.first { it.rateText.isNotEmpty() }.rateText)
    }

    @Test fun commaBecomesDotWhileTyping() = runTest {
        val vm = SettingsViewModel(FakeSettingsRepository(), repo, FakeLanguageController()); vm.onRateChange("25,5")
        assertEquals("25.5", vm.uiState.value.rateText)
    }

    @Test fun savesValidRateAndReformats() = runTest {
        val s = FakeSettingsRepository(null); val vm = SettingsViewModel(s, repo, FakeLanguageController())
        vm.onRateChange("25,5"); vm.saveRate()
        assertEquals(2550L, s.hourlyRateCents.first()); assertFalse(vm.uiState.value.rateError)
        assertEquals("25.50", vm.uiState.value.rateText)
    }

    @Test fun timeZoneChange() = runTest {
        val s = FakeSettingsRepository(); val vm = SettingsViewModel(s, repo, FakeLanguageController())
        assertEquals("Europe/Rome", vm.uiState.first().timeZoneId)
        vm.onTimeZoneSelected("Europe/Zurich"); assertEquals("Europe/Zurich", s.timeZoneId.first())
    }

    @Test fun unknownTimeZoneIgnored() = runTest {
        val s = FakeSettingsRepository(); val vm = SettingsViewModel(s, repo, FakeLanguageController())
        vm.onTimeZoneSelected("Nowhere/Land"); assertEquals("Europe/Rome", s.timeZoneId.first())
    }

    @Test fun exportFileNameUsesZone() = runTest {
        val clock = Clock.fixed(Instant.parse("2026-10-19T22:30:00Z"), ZoneOffset.UTC)
        assertEquals("karvo-lessons-2026-10-20.csv", SettingsViewModel(FakeSettingsRepository(), repo, FakeLanguageController(), clock).exportFileName())
    }

    @Test fun zoneListHasRome() = assertTrue("Europe/Rome" in availableTimeZones)

    @Test fun invalidRateShowsErrorAndKeepsOld() = runTest {
        val s = FakeSettingsRepository(1000); val vm = SettingsViewModel(s, repo, FakeLanguageController())
        vm.onRateChange("abc"); vm.saveRate()
        assertTrue(vm.uiState.value.rateError); assertEquals(1000L, s.hourlyRateCents.first())
    }

    @Test fun blankClearsRate() = runTest {
        val s = FakeSettingsRepository(1000); val vm = SettingsViewModel(s, repo, FakeLanguageController())
        vm.onRateChange(""); vm.saveRate(); assertNull(s.hourlyRateCents.first())
    }

    @Test fun csvContainsLessons() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 10, 20), durationMinutes = 90, student = "Anna"))
        assertTrue(SettingsViewModel(FakeSettingsRepository(), repo, FakeLanguageController()).buildCsv().contains("2026-10-20,Anna,90,1.5,"))
    }

    @Test fun saveRateTwiceIsIdempotent() = runTest {
        val s = FakeSettingsRepository(null); val vm = SettingsViewModel(s, repo, FakeLanguageController())
        vm.onRateChange("30"); vm.saveRate(); vm.saveRate()
        assertEquals(3000L, s.hourlyRateCents.first()); assertEquals(1, s.rateWrites)
    }

    @Test fun saveRateWithoutEditDoesNotWrite() = runTest {
        val s = FakeSettingsRepository(2500); val vm = SettingsViewModel(s, repo, FakeLanguageController())
        vm.saveRate()
        assertEquals(2500L, s.hourlyRateCents.first()); assertEquals(0, s.rateWrites)
    }

    @Test fun typedRateSavedWithoutDone() = runTest {
        val s = FakeSettingsRepository(null); val vm = SettingsViewModel(s, repo, FakeLanguageController())
        vm.onRateChange("30")
        vm.saveRate() // what the screen's onDispose / ON_STOP path calls
        assertEquals(3000L, s.hourlyRateCents.first())
    }

    @Test fun themeSelectionPersistsAndShows() = runTest {
        val settings = FakeSettingsRepository()
        val vm = SettingsViewModel(settings, repo, FakeLanguageController())
        vm.onThemeSelected(ThemeChoice.CLASSIC)
        assertEquals(ThemeChoice.CLASSIC, settings.theme.first())
        assertEquals(ThemeChoice.CLASSIC, vm.uiState.first { it.theme == ThemeChoice.CLASSIC }.theme)
    }

    @Test fun languageShowsCurrentAndSetsController() = runTest {
        val lang = FakeLanguageController(LanguageChoice.ITALIAN)
        val vm = SettingsViewModel(FakeSettingsRepository(), repo, lang)
        assertEquals(LanguageChoice.ITALIAN, vm.uiState.value.language)
        vm.onLanguageSelected(LanguageChoice.ENGLISH)
        assertEquals(LanguageChoice.ENGLISH, lang.current())
        assertEquals(LanguageChoice.ENGLISH, vm.uiState.value.language)
    }

    @Test fun languageChangeSavesTypedRateFirst() = runTest {
        val settings = FakeSettingsRepository()
        val vm = SettingsViewModel(settings, repo, FakeLanguageController())
        vm.onRateChange("30")
        vm.onLanguageSelected(LanguageChoice.ITALIAN)
        assertEquals(3000L, settings.hourlyRateCents.first { it != null })
    }

    @Test fun settingSeasonStartPersists() = runTest {
        val s = FakeSettingsRepository(); val vm = SettingsViewModel(s, repo, FakeLanguageController())
        assertEquals(11, vm.uiState.value.seasonStartMonth)
        vm.onSeasonStartSelected(12)
        assertEquals(12, s.seasonStartMonth.first())
        assertEquals(12, vm.uiState.value.seasonStartMonth)
    }

    @Test fun turningSettingOffNeedsAuth() = runTest {
        val s = FakeSettingsRepository(); val auth = FakeAuthenticator(succeeds = false)
        val vm = SettingsViewModel(s, repo, FakeLanguageController(), authenticator = auth)
        assertTrue(vm.uiState.value.biometricReveal)
        vm.onBiometricRevealChange(false, "Show earnings")
        assertTrue(s.biometricReveal.first())
        auth.succeeds = true
        vm.onBiometricRevealChange(false, "Show earnings")
        assertFalse(s.biometricReveal.first())
        assertFalse(vm.uiState.value.biometricReveal)
    }

    @Test fun turningSettingOnNeedsNoAuth() = runTest {
        val s = FakeSettingsRepository().apply { setBiometricReveal(false) }; val auth = FakeAuthenticator()
        val vm = SettingsViewModel(s, repo, FakeLanguageController(), authenticator = auth)
        vm.onBiometricRevealChange(true, "Show earnings")
        assertTrue(s.biometricReveal.first())
        assertEquals(0, auth.calls)
    }

    @Test fun availabilityShownAndRefreshed() = runTest {
        val auth = FakeAuthenticator(available = false)
        val vm = SettingsViewModel(FakeSettingsRepository(), repo, FakeLanguageController(), authenticator = auth)
        assertFalse(vm.uiState.value.biometricAvailable)
        auth.available = true
        vm.refreshBiometricAvailability()
        assertTrue(vm.uiState.value.biometricAvailable)
    }
}
