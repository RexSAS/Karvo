package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import java.time.YearMonth

/** How a student is matched across lessons: ignoring case and surrounding spaces (as the Overview's student count does). */
fun studentKey(name: String): String = name.trim().lowercase()

data class StudentMonth(val month: YearMonth, val minutes: Long, val lessons: List<Lesson>)

data class StudentDetail(
    /** The spelling used most recently. */
    val name: String,
    val totalMinutes: Long,
    val lessonCount: Int,
    /** Rounded half-up; 0 when there are no lessons. */
    val averageMinutes: Long,
    val firstLesson: LocalDate?,
    val lastLesson: LocalDate?,
    /** Null when no rate is set. */
    val earningsCents: Long?,
    /** Exactly 6, oldest first, ending at today's month; empty months are 0. */
    val lastSixMonths: List<MonthBar>,
    val season: Season,
    val thisSeasonMinutes: Long,
    val lastSeasonMinutes: Long,
    /** Months with lessons, newest first; each month's lessons newest first. */
    val months: List<StudentMonth>,
)

/** Everything about the student matching [key] (see [studentKey]); null when they have no lessons. */
fun computeStudent(lessons: List<Lesson>, key: String, today: LocalDate, seasonStartMonth: Int, rateCents: Long?): StudentDetail? {
    val theirs = lessons.filter { studentKey(it.student) == key }
    if (theirs.isEmpty()) return null
    val total = theirs.sumOf { it.durationMinutes.toLong() }
    val n = theirs.size.toLong()
    val byMonth = theirs.groupBy { YearMonth.from(it.date) }
    val currentMonth = YearMonth.from(today)
    val season = seasonOf(today, seasonStartMonth)
    val lastSeason = Season(season.startYear - 1, seasonStartMonth)
    fun minutesIn(s: Season) = theirs.filter { it.date in s.range() }.sumOf { it.durationMinutes.toLong() }
    val newestFirst = compareByDescending<Lesson> { it.date }.thenByDescending { it.id }
    return StudentDetail(
        name = theirs.maxWith(compareBy<Lesson> { it.date }.thenBy { it.id }).student.trim(),
        totalMinutes = total,
        lessonCount = theirs.size,
        averageMinutes = (total * 2 + n) / (2 * n),
        firstLesson = theirs.minOf { it.date },
        lastLesson = theirs.maxOf { it.date },
        earningsCents = rateCents?.let { earningsCents(total, it) },
        lastSixMonths = (5 downTo 0).map { back ->
            val month = currentMonth.minusMonths(back.toLong())
            val minutes = byMonth[month].orEmpty().sumOf { it.durationMinutes.toLong() }
            MonthBar(month, minutes, rateCents?.let { earningsCents(minutes, it) })
        },
        season = season,
        thisSeasonMinutes = minutesIn(season),
        lastSeasonMinutes = minutesIn(lastSeason),
        months = byMonth.entries.sortedByDescending { it.key }.map { (month, items) ->
            StudentMonth(month, items.sumOf { it.durationMinutes.toLong() }, items.sortedWith(newestFirst))
        },
    )
}
