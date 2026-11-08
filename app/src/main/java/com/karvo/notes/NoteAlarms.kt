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
import com.karvo.data.EventRepository
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
class NoteAlarms(private val context: Context, private val events: EventRepository, private val settings: SettingsRepository) {
    private val app = context.applicationContext

    private val prefs = app.getSharedPreferences("note_alarms", Context.MODE_PRIVATE)
    private var lastNotified: LocalDate?
        get() = prefs.getString("last", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        set(v) { prefs.edit().putString("last", v?.toString()).apply() }

    fun start(scope: CoroutineScope) {
        // runCatching: one failed arm (e.g. exact permission revoked mid-call) must not stop re-arming for good.
        scope.launch { combine(events.observe(), settings.timeZoneId) { e, z -> e to z }.collect { (e, z) -> runCatching { arm(e.map { it.date }, zoneOf(z)) } } }
        scope.launch { runCatching { catchUp() } }
    }

    suspend fun rearm() { runCatching { arm(events.all().map { it.date }, zoneOf(settings.timeZoneId.first())) }; catchUp() }

    /** After a reboot, a clock change or app start: today's reminder if it was missed. */
    private suspend fun catchUp() {
        if (missedToday(events.all().map { it.date }, ZonedDateTime.now(zoneOf(settings.timeZoneId.first())), lastNotified)) notifyToday()
    }

    private fun zoneOf(id: String) = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())

    private fun arm(all: List<LocalDate>, zone: ZoneId) {
        val am = app.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(app, 0, Intent(app, NoteAlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val next = nextReminder(all, ZonedDateTime.now(zone))
        when {
            next == null -> am.cancel(pi)
            canRingExactly(app) -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second.toEpochMilli(), pi)
            else -> am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second.toEpochMilli(), pi) // Android may delay it up to ~1 h
        }
    }

    /** Posts "Karvo · Today: <events>" when today has events. */
    suspend fun notifyToday() {
        val zone = zoneOf(settings.timeZoneId.first())
        val today = LocalDate.now(zone)
        val text = events.all().filter { it.date == today }.sortedBy { it.id }.joinToString(" · ") { it.text }.ifEmpty { return }
        lastNotified = today
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = app.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, app.getString(R.string.event_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_link_notification)
            .setContentTitle(app.getString(R.string.event_today_title))
            .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true).build())
    }

    private companion object { const val CHANNEL = "events"; const val NOTIFICATION_ID = 5207 }
}

/** The 07:30 alarm: notify, then arm the next one. */
class NoteAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarms = (context.applicationContext as KarvoApp).container.noteAlarms
        val pending = goAsync()
        CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try { alarms.notifyToday(); alarms.rearm() } finally { pending.finish() } // notifyToday marks today, so rearm won't repeat it
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

/** Whether reminders can ring exactly at 07:30 ("Alarms & reminders" allowed; always true before Android 12). */
fun canRingExactly(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
