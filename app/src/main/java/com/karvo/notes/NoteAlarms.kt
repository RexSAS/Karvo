package com.karvo.notes

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.karvo.KarvoApp
import com.karvo.MainActivity
import com.karvo.R
import com.karvo.data.DayNoteRepository
import com.karvo.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Keeps one AlarmManager alarm at the next note day's 07:30 (inexact, allowed while idle: no exact-alarm permission).
 * Re-armed whenever notes or the app's time zone change, after a reminder rings, and after a reboot or clock change.
 */
class NoteAlarms(private val context: Context, private val notes: DayNoteRepository, private val settings: SettingsRepository) {
    private val app = context.applicationContext

    fun start(scope: CoroutineScope) {
        scope.launch { combine(notes.observe(), settings.timeZoneId) { n, z -> n to z }.collect { (n, z) -> arm(n, zoneOf(z)) } }
    }

    suspend fun rearm() = arm(notes.all(), zoneOf(settings.timeZoneId.first()))

    private fun zoneOf(id: String) = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())

    private fun arm(all: Map<LocalDate, String>, zone: ZoneId) {
        val am = app.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(app, 0, Intent(app, NoteAlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val next = nextReminder(all, ZonedDateTime.now(zone))
        if (next == null) am.cancel(pi) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second.toEpochMilli(), pi)
    }

    /** Posts "Karvo · Today: <note>" when today has a note. */
    suspend fun notifyToday() {
        val zone = zoneOf(settings.timeZoneId.first())
        val text = notes.all()[LocalDate.now(zone)] ?: return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = app.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, app.getString(R.string.note_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_link_notification)
            .setContentTitle(app.getString(R.string.note_today_title))
            .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true).build())
    }

    private companion object { const val CHANNEL = "day_notes"; const val NOTIFICATION_ID = 5207 }
}

/** The 07:30 alarm: notify, then arm the next one. */
class NoteAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarms = (context.applicationContext as KarvoApp).container.noteAlarms
        val pending = goAsync()
        CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try { alarms.notifyToday(); alarms.rearm() } finally { pending.finish() }
        }
    }
}

/** Alarms are cleared by a reboot and shift with clock changes: arm again. */
class NoteRearmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarms = (context.applicationContext as KarvoApp).container.noteAlarms
        val pending = goAsync()
        CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch { try { alarms.rearm() } finally { pending.finish() } }
    }
}
