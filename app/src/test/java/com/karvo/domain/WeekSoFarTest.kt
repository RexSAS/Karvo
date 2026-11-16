package com.karvo.domain

import com.karvo.data.Lesson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekSoFarTest {
    private fun lesson(date: String, minutes: Int) = Lesson(date = LocalDate.parse(date), durationMinutes = minutes, student = "A")

    // 2026-11-16 is a Monday.
    private val lessons = listOf(lesson("2026-11-15", 120), lesson("2026-11-16", 90), lesson("2026-11-18", 60), lesson("2026-11-23", 30))

    @Test fun mondayCountsOnlyMonday() = assertEquals(90L to 1, weekSoFar(lessons, LocalDate.parse("2026-11-16")))
    @Test fun sundayCountsTheWholeWeek() = assertEquals(150L to 2, weekSoFar(lessons, LocalDate.parse("2026-11-22")))
    @Test fun emptyMondayIsZero() = assertEquals(0L to 0, weekSoFar(lessons.take(1), LocalDate.parse("2026-11-16")))
    @Test fun monthCountsFromTheFirstToToday() = assertEquals(270L to 3, monthSoFar(lessons, LocalDate.parse("2026-11-18")))
    @Test fun runningTotalsAddUpFromMonday() =
        assertEquals(listOf(90L, 90L, 150L, 150L, 150L, 150L, 150L), weekRunningTotals(lessons, LocalDate.parse("2026-11-19")))
    @Test fun runningTotalsOfLastWeekEndOnItsSunday() =
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L, 0L, 120L), weekRunningTotals(lessons, LocalDate.parse("2026-11-09")))
    @Test fun monthLeavesOutLastMonth() = assertEquals(0L to 0, monthSoFar(listOf(lesson("2026-10-31", 60)), LocalDate.parse("2026-11-01")))
}
