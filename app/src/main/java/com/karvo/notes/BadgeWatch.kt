package com.karvo.notes

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.karvo.MainActivity
import com.karvo.R
import com.karvo.data.LessonRepository
import com.karvo.data.SettingsRepository
import com.karvo.domain.Badge
import com.karvo.domain.Season
import com.karvo.domain.badgeMark
import com.karvo.domain.badgeProgress
import com.karvo.domain.badgeToAnnounce
import com.karvo.domain.formatDuration
import com.karvo.domain.seasonOf
import com.karvo.ui.components.badgeBitmap
import com.karvo.ui.components.titleRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Follows this season's hours, whoever adds the lessons (this phone, the PC link, a restore). A new badge posts a
 * notification and waits in [celebrate] until the app has shown its unlock animation. On the very first run the badge
 * already earned is not news for a notification, but the app still celebrates it once.
 */
class BadgeWatch(
    context: Context,
    private val lessons: LessonRepository,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("badges", Context.MODE_PRIVATE)
    private val pending = MutableStateFlow<Badge?>(null)
    /** A badge unlocked and not yet celebrated in the app. */
    val celebrate: StateFlow<Badge?> = pending
    private var now: Triple<Season, Badge?, Long>? = null

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(lessons.observeLessons(), settings.timeZoneId, settings.seasonStartMonth) { all, zone, start ->
                val today = LocalDate.now(clock.withZone(runCatching { ZoneId.of(zone) }.getOrDefault(ZoneId.systemDefault())))
                val season = seasonOf(today, start)
                val minutes = all.filter { it.date in season.range() }.sumOf { it.durationMinutes.toLong() }
                Triple(season, badgeProgress(minutes).earned, minutes)
            }.distinctUntilChanged { a, b -> a.first == b.first && a.second == b.second }
                .collect { runCatching { check(it) } }
        }
    }

    private fun check(state: Triple<Season, Badge?, Long>) {
        now = state
        val (season, earned, minutes) = state
        val notified = prefs.getString(NOTIFIED, null)
        if (notified == null) {
            prefs.edit().putString(NOTIFIED, badgeMark(season, earned)).apply()
        } else badgeToAnnounce(season, earned, notified)?.let {
            prefs.edit().putString(NOTIFIED, badgeMark(season, it)).apply()
            post(it, minutes)
        }
        pending.value = badgeToAnnounce(season, earned, prefs.getString(CELEBRATED, null))
    }

    /** The app showed the unlock animation for [badge]. */
    fun celebrated(badge: Badge) {
        val season = now?.first ?: return
        prefs.edit().putString(CELEBRATED, badgeMark(season, badge)).apply()
        if (pending.value == badge) pending.value = null
    }

    /** Test app only: forget this season's badges, so the one earned is notified and celebrated again. */
    fun forget() {
        val state = now ?: return
        prefs.edit().putString(NOTIFIED, badgeMark(state.first, null)).remove(CELEBRATED).apply()
        check(state)
    }

    private fun post(badge: Badge, minutes: Long) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = app.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, app.getString(R.string.badge_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(app, 2, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val next = badgeProgress(minutes).next
        val text = app.getString(R.string.badge_unlocked_text, formatDuration(minutes)) +
            (next?.let { " " + app.getString(R.string.badge_next, app.getString(it.titleRes()), formatDuration(it.minutes)) } ?: "")
        NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_link_notification)
            .setLargeIcon(badgeBitmap(badge, 256).asAndroidBitmap())
            .setContentTitle(app.getString(R.string.badge_unlocked_title, app.getString(badge.titleRes())))
            .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true).build())
    }

    private companion object {
        const val NOTIFIED = "notified"
        const val CELEBRATED = "celebrated"
        const val CHANNEL = "badges"
        const val NOTIFICATION_ID = 5209
    }
}
