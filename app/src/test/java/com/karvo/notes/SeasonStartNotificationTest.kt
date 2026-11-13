package com.karvo.notes

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.karvo.testing.FakeEventRepository
import com.karvo.testing.FakeSettingsRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeasonStartNotificationTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val posted get() = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications

    @Before fun allowNotifications() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        app.getSharedPreferences("note_alarms", 0).edit().clear().commit()
    }

    private fun alarms(at: String) =
        NoteAlarms(app, FakeEventRepository(), FakeSettingsRepository(null, zone = "UTC"), Clock.fixed(Instant.parse(at), ZoneOffset.UTC))

    @Test fun the_seasons_first_day_says_it_is_starting_once() = runTest {
        val alarms = alarms("2026-12-01T07:30:00Z") // seasons start in December by default
        alarms.notifySeasonStart()
        alarms.notifySeasonStart() // the alarm and the catch-up must not post it twice
        assertEquals(1, posted.size)
        val n = shadowOf(posted.single())
        assertEquals("Season is starting!", n.contentTitle)
        assertEquals("Enjoy the slopes!", n.contentText)
    }

    @Test fun any_other_day_stays_quiet() = runTest {
        alarms("2026-12-02T07:30:00Z").notifySeasonStart()
        alarms("2026-11-30T07:30:00Z").notifySeasonStart()
        assertEquals(0, posted.size)
    }
}
