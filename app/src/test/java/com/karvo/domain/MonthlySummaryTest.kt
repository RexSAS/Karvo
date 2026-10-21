package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test
import org.junit.Assert.assertEquals

class MonthlySummaryTest {
    private fun l(y: Int, m: Int, d: Int, min: Int) = Lesson(date = LocalDate.of(y, m, d), durationMinutes = min, student = "A")

    @Test fun groupsAndSortsNewestFirst() {
        val r = summarizeByMonth(listOf(l(2026, 1, 5, 60), l(2027, 1, 3, 30), l(2026, 12, 31, 90), l(2026, 1, 20, 120)))
        assertEquals(listOf(YearMonth.of(2027, 1), YearMonth.of(2026, 12), YearMonth.of(2026, 1)), r.map { it.month })
        assertEquals(MonthSummary(YearMonth.of(2026, 1), 180, 2), r[2])
    }

    @Test fun empty() = assertEquals(emptyList<MonthSummary>(), summarizeByMonth(emptyList()))
}
