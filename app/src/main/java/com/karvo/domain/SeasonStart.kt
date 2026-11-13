package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate

/** The first days of this year's and next year's seasons: the days the "Season is starting!" reminder rings. */
fun seasonStartDays(today: LocalDate, startMonth: Int): List<LocalDate> =
    listOf(LocalDate.of(today.year, startMonth, 1), LocalDate.of(today.year + 1, startMonth, 1))

/** The card shown the first time Karvo is opened in a new season. */
data class SeasonWelcome(
    val season: Season,
    /** The season before [season]: its totals, or null when it had no lessons. */
    val last: SeasonTotal?,
    val lastStudents: Int,
    /** Months of the last season not (fully) paid yet. */
    val lastUnpaidMonths: Int,
)

/**
 * The welcome card for the season containing [today], or null when there is none to show. [welcomedSeason] is the
 * start year of the last season already welcomed: null means never (a fresh install, or an update to this version),
 * and then nothing is shown, so the card only ever appears when a season really begins. No lessons at all, no card.
 * Whatever the answer, the caller then stores today's season as welcomed.
 */
fun seasonWelcome(lessons: List<Lesson>, today: LocalDate, startMonth: Int, welcomedSeason: Int?, rateCents: Long?): SeasonWelcome? {
    val season = seasonOf(today, startMonth)
    if (welcomedSeason == null || welcomedSeason >= season.startYear || lessons.isEmpty()) return null
    val lastSeason = Season(season.startYear - 1, startMonth)
    val inLast = lessons.filter { it.date in lastSeason.range() }
    val minutes = inLast.sumOf { it.durationMinutes.toLong() }
    return SeasonWelcome(
        season = season,
        last = if (inLast.isEmpty()) null else SeasonTotal(lastSeason, minutes, rateCents?.let { earningsCents(minutes, it) }, inLast.size),
        lastStudents = inLast.map { studentKey(it.student) }.distinct().size,
        lastUnpaidMonths = summarizeByMonth(inLast).count { it.paidStatus != PaidStatus.PAID },
    )
}
