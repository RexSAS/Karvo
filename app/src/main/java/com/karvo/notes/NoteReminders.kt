package com.karvo.notes

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Day-note reminders ring at 07:30 local time on the note's day. */
val REMINDER_TIME: LocalTime = LocalTime.of(7, 30)

/** The next note day whose 07:30 is still ahead of [now], with that instant; null when none is left. */
fun nextReminder(notes: Map<LocalDate, String>, now: ZonedDateTime): Pair<LocalDate, Instant>? =
    notes.keys.sorted().asSequence()
        .map { it to it.atTime(REMINDER_TIME).atZone(now.zone).toInstant() }
        .firstOrNull { it.second.isAfter(now.toInstant()) }

/** Today has a note, 07:30 has passed and it wasn't shown yet (phone off or locked at 07:30): show it once now. */
fun missedToday(notes: Map<LocalDate, String>, now: ZonedDateTime, lastNotified: LocalDate?): Boolean {
    val today = now.toLocalDate()
    return today in notes && !now.toLocalTime().isBefore(REMINDER_TIME) && lastNotified != today
}
