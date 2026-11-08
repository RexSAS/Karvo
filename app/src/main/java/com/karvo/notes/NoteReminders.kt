package com.karvo.notes

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Event reminders ring at 07:30 local time on days with events. */
val REMINDER_TIME: LocalTime = LocalTime.of(7, 30)

/** The next event day whose 07:30 is still ahead of [now], with that instant; null when none is left. */
fun nextReminder(days: Collection<LocalDate>, now: ZonedDateTime): Pair<LocalDate, Instant>? =
    days.toSortedSet().asSequence()
        .map { it to it.atTime(REMINDER_TIME).atZone(now.zone).toInstant() }
        .firstOrNull { it.second.isAfter(now.toInstant()) }

/** Today has events, 07:30 has passed and the reminder wasn't shown yet (phone off or locked at 07:30): show it once now. */
fun missedToday(days: Collection<LocalDate>, now: ZonedDateTime, lastNotified: LocalDate?): Boolean {
    val today = now.toLocalDate()
    return today in days && !now.toLocalTime().isBefore(REMINDER_TIME) && lastNotified != today
}
