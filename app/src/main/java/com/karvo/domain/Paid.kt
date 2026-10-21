package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import java.time.YearMonth

enum class PaidStatus { UNPAID, PARTLY_PAID, PAID }

/** A month is paid when every lesson has a paid date; an empty list counts as unpaid. */
fun paidStatus(lessons: List<Lesson>): PaidStatus {
    val paid = lessons.count { it.paidOn != null }
    return when {
        paid == 0 -> PaidStatus.UNPAID
        paid == lessons.size -> PaidStatus.PAID
        else -> PaidStatus.PARTLY_PAID
    }
}

/** What the school still owes: unpaid lessons up to [today] (future lessons aren't owed yet). */
data class Unpaid(val months: Int, val minutes: Long, val earningsCents: Long?)

fun unpaidTotals(lessons: List<Lesson>, today: LocalDate, rateCents: Long?): Unpaid {
    val owed = lessons.filter { it.paidOn == null && !it.date.isAfter(today) }
    val minutes = owed.sumOf { it.durationMinutes.toLong() }
    return Unpaid(
        months = owed.map { YearMonth.from(it.date) }.distinct().size,
        minutes = minutes,
        earningsCents = rateCents?.let { earningsCents(minutes, it) },
    )
}
