package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import java.time.YearMonth

data class MonthBar(val month: YearMonth, val minutes: Long, val earningsCents: Long?)

data class StudentStat(val name: String, val minutes: Long, val earningsCents: Long?)

data class OverviewStats(
    val lessonsThisMonth: Int,
    /** Over all lessons, rounded half-up; 0 when there are none. */
    val averageLessonMinutes: Long,
    /** Distinct students, matched ignoring case and surrounding spaces. */
    val studentCount: Int,
    /** Exactly 6 bars, oldest first, ending at the chart's last month (normally today's); empty months are 0. */
    val lastSixMonths: List<MonthBar>,
    /** Earnings for [lastSixMonths], each lesson at its rate; null when no rate is set. */
    val sixMonthEarningsCents: Long?,
    /** Up to 5, by minutes descending, then name. */
    val topStudents: List<StudentStat>,
    /** Exactly 7 counts, Monday first. */
    val weekdayCounts: List<Int>,
)

private const val TOP_STUDENTS = 5

/** [lastMonth] is where the 6-month chart ends: today's month, or the last month of a past season. */
fun computeStats(lessons: List<Lesson>, today: LocalDate, pay: Pay, lastMonth: YearMonth = YearMonth.from(today)): OverviewStats {
    val currentMonth = YearMonth.from(today)
    val byMonth = lessons.groupBy { YearMonth.from(it.date) }
    val sixMonths = (5 downTo 0).map { lastMonth.minusMonths(it.toLong()) }
    val lastSixMonths = sixMonths.map { month ->
        val items = byMonth[month].orEmpty()
        MonthBar(month, items.sumOf { it.durationMinutes.toLong() }, pay.earnings(items))
    }

    val students = lessons.groupBy { it.student.trim().lowercase() }.values.map { items ->
        val minutes = items.sumOf { it.durationMinutes.toLong() }
        // Display the spelling used most recently.
        val name = items.maxWith(compareBy<Lesson> { it.date }.thenBy { it.id }).student.trim()
        StudentStat(name, minutes, pay.earnings(items))
    }

    val n = lessons.size.toLong()
    val total = lessons.sumOf { it.durationMinutes.toLong() }
    val weekdayCounts = IntArray(7).also { counts -> lessons.forEach { counts[it.date.dayOfWeek.value - 1]++ } }

    return OverviewStats(
        lessonsThisMonth = lessons.count { YearMonth.from(it.date) == currentMonth },
        averageLessonMinutes = if (n == 0L) 0L else (total * 2 + n) / (2 * n),
        studentCount = students.size,
        lastSixMonths = lastSixMonths,
        sixMonthEarningsCents = pay.earnings(sixMonths.flatMap { byMonth[it].orEmpty() }),
        topStudents = students.sortedWith(compareByDescending<StudentStat> { it.minutes }.thenBy { it.name }).take(TOP_STUDENTS),
        weekdayCounts = weekdayCounts.toList(),
    )
}
