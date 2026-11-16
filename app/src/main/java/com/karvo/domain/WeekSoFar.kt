package com.karvo.domain

import com.karvo.data.Lesson
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Minutes and lessons from this week's Monday through [today]; on a Monday with nothing logged it is 0. */
fun weekSoFar(lessons: List<Lesson>, today: LocalDate): Pair<Long, Int> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val week = lessons.filter { it.date in monday..today }
    return week.sumOf { it.durationMinutes.toLong() } to week.size
}
