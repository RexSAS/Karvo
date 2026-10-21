package com.karvo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var repo: DataStoreSettingsRepository
    private lateinit var store: DataStore<Preferences>

    @Before fun setUp() {
        store = PreferenceDataStoreFactory.create { File(tmp.root, "settings.preferences_pb") }
        repo = DataStoreSettingsRepository(store)
    }

    @Test fun setAndClear() = runTest {
        assertNull(repo.hourlyRateCents.first())
        repo.setHourlyRateCents(2550); assertEquals(2550L, repo.hourlyRateCents.first())
        repo.setHourlyRateCents(null); assertNull(repo.hourlyRateCents.first())
    }

    @Test fun timeZoneDefaultsToRome() = runTest { assertEquals("Europe/Rome", repo.timeZoneId.first()) }

    @Test fun timeZoneSet() = runTest {
        repo.setTimeZoneId("Europe/Zurich"); assertEquals("Europe/Zurich", repo.timeZoneId.first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidZoneRejected() = runTest { repo.setTimeZoneId("Mars/Base") }

    @Test fun invalidStoredZoneFallsBackToDefault() = runTest {
        store.edit { it[stringPreferencesKey("time_zone_id")] = "Mars/Base" }
        assertEquals("Europe/Rome", repo.timeZoneId.first())
    }

    @Test fun themeDefaultsToAlpineNight() = runTest { assertEquals(ThemeChoice.ALPINE_NIGHT, repo.theme.first()) }

    @Test fun themeRoundTrips() = runTest {
        repo.setTheme(ThemeChoice.CLASSIC); assertEquals(ThemeChoice.CLASSIC, repo.theme.first())
    }

    @Test fun unknownThemeFallsBack() = runTest {
        store.edit { it[stringPreferencesKey("theme")] = "NEON" }
        assertEquals(ThemeChoice.ALPINE_NIGHT, repo.theme.first())
    }
}
