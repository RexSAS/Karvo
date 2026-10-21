package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate

/** A ski season: twelve months from the 1st of [startMonth] in [startYear]. */
data class Season(val startYear: Int, val startMonth: Int) {
    fun range(): ClosedRange<LocalDate> {
        val start = LocalDate.of(startYear, startMonth, 1)
        return start..start.plusYears(1).minusDays(1)
    }

    /** "2026/27" when the season crosses the new year, "2026" when it starts in January. */
    fun label(): String =
        if (startMonth == 1) "$startYear" else "$startYear/" + ((startYear + 1) % 100).toString().padStart(2, '0')
}

fun seasonOf(date: LocalDate, startMonth: Int): Season =
    Season(if (date.monthValue >= startMonth) date.year else date.year - 1, startMonth)

data class SeasonTotal(val season: Season, val minutes: Long, val earningsCents: Long?, val lessonCount: Int)

data class SeasonStats(
    /** The season containing today; may have no lessons. */
    val current: SeasonTotal,
    /** Last season's minutes up to the same date a year ago; null when last season has no lessons. */
    val previousToDateMinutes: Long?,
    /** Seasons with at least one lesson, newest first. */
    val all: List<SeasonTotal>,
)

fun computeSeasons(lessons: List<Lesson>, today: LocalDate, startMonth: Int, rateCents: Long?): SeasonStats {
    fun total(season: Season, items: List<Lesson>): SeasonTotal {
        val minutes = items.sumOf { it.durationMinutes.toLong() }
        return SeasonTotal(season, minutes, rateCents?.let { earningsCents(minutes, it) }, items.size)
    }

    val bySeason = lessons.groupBy { seasonOf(it.date, startMonth) }
    val current = seasonOf(today, startMonth)
    val previous = Season(current.startYear - 1, startMonth)
    // minusYears turns 29 Feb into 28 Feb, so the comparison never reaches past the same calendar point.
    val sameDayLastYear = today.minusYears(1)
    val previousToDate = bySeason[previous]?.let { items ->
        items.filter { !it.date.isAfter(sameDayLastYear) }.sumOf { it.durationMinutes.toLong() }
    }
    return SeasonStats(
        current = total(current, bySeason[current].orEmpty()),
        previousToDateMinutes = previousToDate,
        all = bySeason.map { (season, items) -> total(season, items) }.sortedByDescending { it.season.startYear },
    )
}
