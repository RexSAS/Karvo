package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import java.time.YearMonth

/** [paidOn] is the latest paid date among the month's lessons, or null when none is paid. */
data class MonthSummary(
    val month: YearMonth,
    val totalMinutes: Long,
    val lessonCount: Int,
    val paidStatus: PaidStatus = PaidStatus.UNPAID,
    val paidOn: LocalDate? = null,
)

fun summarizeByMonth(lessons: List<Lesson>): List<MonthSummary> =
    lessons.groupBy { YearMonth.from(it.date) }
        .map { (month, items) ->
            MonthSummary(
                month,
                items.sumOf { it.durationMinutes.toLong() },
                items.size,
                paidStatus(items),
                items.mapNotNull { it.paidOn }.maxOrNull(),
            )
        }
        .sortedByDescending { it.month }
