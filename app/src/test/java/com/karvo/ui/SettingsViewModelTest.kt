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
    private val rates = com.karvo.data.RateRepository(com.karvo.testing.FakeRateDao(repo))

    @Test fun ratesAreListedAddedMadeDefaultAndDeleted() = runTest {
        val vm = SettingsViewModel(FakeSettingsRepository(), repo, FakeLanguageController(), rates)
        vm.saveRate(com.karvo.data.Rate(title = "Standard", cents = 3000, color = 1))
        vm.saveRate(com.karvo.data.Rate(title = "Private", cents = 4500, color = 2))
        assertEquals(listOf("Standard", "Private"), vm.uiState.first { it.rates.size == 2 }.rates.map { it.title })
        val private = rates.all().single { it.title == "Private" }
        vm.setDefaultRate(private)
        assertEquals("Private", vm.uiState.first { it.rates.first().title == "Private" }.rates.first().title)
        vm.deleteRate(rates.all().single { it.title == "Standard" })
        assertEquals(listOf("Private"), vm.uiState.first { it.rates.size == 1 }.rates.map { it.title })
    }

    @Test fun csvNamesEachLessonsRate() = runTest {
        rates.save(com.karvo.data.Rate(title = "Standard", cents = 3000, color = 1))
        repo.save(Lesson(date = LocalDate.of(2026, 10, 20), durationMinutes = 90, student = "Anna"))
        assertTrue(SettingsViewModel(FakeSettingsRepository(), repo, FakeLanguageController(), rates).buildCsv().contains("2026-10-20,Anna,90,1.5,,,Standard"))
    }

    @Test fun timeZoneChange() = runTest {
        val s = FakeSettingsRepository(); val vm = SettingsViewModel(s, repo, FakeLanguageController(), rates)
        assertEquals("Europe/Rome", vm.uiState.first().timeZoneId)
        vm.onTimeZoneSelected("Europe/Zurich"); assertEquals("Europe/Zurich", s.timeZoneId.first())
    }

    @Test fun unknownTimeZoneIgnored() = runTest {
        val s = FakeSettingsRepository(); val vm = SettingsViewModel(s, repo, FakeLanguageController(), rates)
        vm.onTimeZoneSelected("Nowhere/Land"); assertEquals("Europe/Rome", s.timeZoneId.first())
    }

    @Test fun exportFileNameUsesZone() = runTest {
        val clock = Clock.fixed(Instant.parse("2026-10-19T22:30:00Z"), ZoneOffset.UTC)
        assertEquals("karvo-lessons-2026-10-20.csv", SettingsViewModel(FakeSettingsRepository(), repo, FakeLanguageController(), rates, clock).exportFileName())
    }

    @Test fun zoneListHasRome() = assertTrue("Europe/Rome" in availableTimeZones)

    @Test fun csvContainsLessons() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 10, 20), durationMinutes = 90, student = "Anna"))
        assertTrue(SettingsViewModel(FakeSettingsRepository(), repo, FakeLanguageController(), rates).buildCsv().contains("2026-10-20,Anna,90,1.5,"))
    }

    @Test fun themeSelectionPersistsAndShows() = runTest {
        val settings = FakeSettingsRepository()
        val vm = SettingsViewModel(settings, repo, FakeLanguageController(), rates)
        vm.onThemeSelected(ThemeChoice.CLASSIC)
        assertEquals(ThemeChoice.CLASSIC, settings.theme.first())
        assertEquals(ThemeChoice.CLASSIC, vm.uiState.first { it.theme == ThemeChoice.CLASSIC }.theme)
    }

    @Test fun languageShowsCurrentAndSetsController() = runTest {
        val lang = FakeLanguageController(LanguageChoice.ITALIAN)
        val vm = SettingsViewModel(FakeSettingsRepository(), repo, lang, rates)
        assertEquals(LanguageChoice.ITALIAN, vm.uiState.value.language)
        vm.onLanguageSelected(LanguageChoice.ENGLISH)
        assertEquals(LanguageChoice.ENGLISH, lang.current())
        assertEquals(LanguageChoice.ENGLISH, vm.uiState.value.language)
    }

    @Test fun settingSeasonStartPersists() = runTest {
        val s = FakeSettingsRepository(); val vm = SettingsViewModel(s, repo, FakeLanguageController(), rates)
        assertEquals(12, vm.uiState.value.seasonStartMonth)
        vm.onSeasonStartSelected(11)
        assertEquals(11, s.seasonStartMonth.first())
        assertEquals(11, vm.uiState.value.seasonStartMonth)
    }

    @Test fun turningSettingOffNeedsAuth() = runTest {
        val s = FakeSettingsRepository(); val auth = FakeAuthenticator(succeeds = false)
        val vm = SettingsViewModel(s, repo, FakeLanguageController(), rates, authenticator = auth)
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
        val vm = SettingsViewModel(s, repo, FakeLanguageController(), rates, authenticator = auth)
        vm.onBiometricRevealChange(true, "Show earnings")
        assertTrue(s.biometricReveal.first())
        assertEquals(0, auth.calls)
    }

    @Test fun availabilityShownAndRefreshed() = runTest {
        val auth = FakeAuthenticator(available = false)
        val vm = SettingsViewModel(FakeSettingsRepository(), repo, FakeLanguageController(), rates, authenticator = auth)
        assertFalse(vm.uiState.value.biometricAvailable)
        auth.available = true
        vm.refreshBiometricAvailability()
        assertTrue(vm.uiState.value.biometricAvailable)
    }
}
