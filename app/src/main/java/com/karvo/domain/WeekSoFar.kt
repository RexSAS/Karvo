package com.karvo.domain

import com.karvo.data.Lesson
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Minutes and lessons from this week's Monday through [today]; on a Monday with nothing logged it is 0. */
fun weekSoFar(lessons: List<Lesson>, today: LocalDate): Pair<Long, Int> =
    totalBetween(lessons, today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today)

/** Minutes added up day by day through [day]'s week, Monday first: 7 running totals. */
fun weekRunningTotals(lessons: List<Lesson>, day: LocalDate): List<Long> {
    val monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L until 7L).map { d -> lessons.filter { it.date in monday..monday.plusDays(d) }.sumOf { it.durationMinutes.toLong() } }
}

/** Minutes and lessons from the 1st of this month through [today]. */
fun monthSoFar(lessons: List<Lesson>, today: LocalDate): Pair<Long, Int> = totalBetween(lessons, today.withDayOfMonth(1), today)

/** Minutes and lessons dated from [from] through [today]. */
fun totalBetween(lessons: List<Lesson>, from: LocalDate, today: LocalDate): Pair<Long, Int> {
    val picked = lessons.filter { it.date in from..today }
    return picked.sumOf { it.durationMinutes.toLong() } to picked.size
}
