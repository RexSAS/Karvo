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
 * event (--es date --es text), events, fireEvent (posts today's reminder now).
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
                    else -> "unknown op"
                }
            }.getOrElse { "ERROR ${it.javaClass.simpleName}: ${it.message}" }
            Log.i("KarvoLinkDebug", "${intent.getStringExtra("op")} -> $out")
            pending.finish()
        }
    }
}
