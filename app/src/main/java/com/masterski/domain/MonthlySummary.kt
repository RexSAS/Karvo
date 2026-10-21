package com.masterski.domain

import com.masterski.data.Lesson
import java.time.YearMonth

data class MonthSummary(val month: YearMonth, val totalMinutes: Long, val lessonCount: Int)

fun summarizeByMonth(lessons: List<Lesson>): List<MonthSummary> =
    lessons.groupBy { YearMonth.from(it.date) }
        .map { (month, items) -> MonthSummary(month, items.sumOf { it.durationMinutes.toLong() }, items.size) }
        .sortedByDescending { it.month }
