package com.karvo.domain

import com.karvo.data.Lesson
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Minutes and lessons from this week's Monday through [today]; on a Monday with nothing logged it is 0. */
fun weekSoFar(lessons: List<Lesson>, today: LocalDate): Pair<Long, Int> =
    soFar(lessons, today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today)

/** Minutes and lessons from the 1st of this month through [today]. */
fun monthSoFar(lessons: List<Lesson>, today: LocalDate): Pair<Long, Int> = soFar(lessons, today.withDayOfMonth(1), today)

private fun soFar(lessons: List<Lesson>, from: LocalDate, today: LocalDate): Pair<Long, Int> {
    val picked = lessons.filter { it.date in from..today }
    return picked.sumOf { it.durationMinutes.toLong() } to picked.size
}
