package com.masterski.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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

    @Before fun setUp() {
        repo = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create { File(tmp.root, "settings.preferences_pb") },
        )
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
}
