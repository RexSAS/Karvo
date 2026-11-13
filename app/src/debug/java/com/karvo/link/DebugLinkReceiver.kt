package com.karvo.link

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.karvo.KarvoApp
import com.karvo.data.Lesson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Debug builds only. `adb shell am broadcast -n com.karvo/.link.DebugLinkReceiver --es op <op> ...`
 * ops: pair (--es qr), add (--es student --es date --ei minutes), list, disconnect, remove (--es room), count,
 * event (--es date --es text), events, fireEvent (posts today's reminder now),
 * fireSeason (posts the "Season is starting!" notification now),
 * seasonCard (adds demo lessons in this and last season, some unpaid, and forgets this season's welcome: reopen the app).
 * Results go to logcat with tag KarvoLinkDebug.
 */
class DebugLinkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = (context.applicationContext as KarvoApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            val out = runCatching {
                when (intent.getStringExtra("op")) {
                    "pair" -> c.linkManager.pair(parseLinkQr(intent.getStringExtra("qr")!!)).toString()
                    "add" -> c.lessonRepository.save(Lesson(0, LocalDate.parse(intent.getStringExtra("date")), intent.getIntExtra("minutes", 60), intent.getStringExtra("student")!!)).toString()
                    "list" -> c.pairingStore.pcs.first().joinToString { "${it.pcName}=${c.linkManager.states.value[it.roomId]}" }
                    "disconnect" -> { c.linkManager.disconnectAll(); "ok" }
                    "remove" -> { c.linkManager.remove(c.pairingStore.pcs.first().first().roomId); "ok" }
                    "count" -> c.lessonRepository.allLessons().size.toString()
                    "event" -> c.eventRepository.add(LocalDate.parse(intent.getStringExtra("date")), intent.getStringExtra("text")!!).toString()
                    "events" -> c.eventRepository.all().joinToString { "${it.date}=${it.text}" }
                    "fireEvent" -> { c.noteAlarms.notifyToday(); "ok" }
                    "fireSeason" -> { c.noteAlarms.postSeasonStart(); "ok" }
                    "seasonCard" -> {
                        val start = c.settingsRepository.seasonStartMonth.first()
                        val season = com.karvo.domain.seasonOf(LocalDate.now(), start)
                        val last = season.startYear - 1
                        // Last season: two paid months and one not; this season: one lesson.
                        listOf(
                            Triple(LocalDate.of(last, start, 6), "Giulia Bianchi", LocalDate.of(last, start, 28)),
                            Triple(LocalDate.of(last, start, 13), "Marco Ferri", LocalDate.of(last, start, 28)),
                            Triple(LocalDate.of(last, start, 1).plusMonths(1).withDayOfMonth(9), "Rossi family", LocalDate.of(last, start, 1).plusMonths(2).withDayOfMonth(5)),
                            Triple(LocalDate.of(last, start, 1).plusMonths(2).withDayOfMonth(14), "Giulia Bianchi", null),
                            Triple(LocalDate.of(last, start, 1).plusMonths(2).withDayOfMonth(15), "Elena Gallo", null),
                            Triple(LocalDate.now(), "Ben Carter", null),
                        ).forEach { (date, student, paid) -> c.lessonRepository.save(Lesson(0, date, 90, student, paidOn = paid)) }
                        c.settingsRepository.setWelcomedSeason(last) // as if this season had just begun
                        "ok: reopen Karvo Test"
                    }
                    else -> "unknown op"
                }
            }.getOrElse { "ERROR ${it.javaClass.simpleName}: ${it.message}" }
            Log.i("KarvoLinkDebug", "${intent.getStringExtra("op")} -> $out")
            pending.finish()
        }
    }
}
