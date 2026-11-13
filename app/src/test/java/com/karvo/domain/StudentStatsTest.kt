package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudentStatsTest {
    private var nextId = 1L
    private fun lesson(date: String, min: Int, student: String) =
        Lesson(id = nextId++, date = LocalDate.parse(date), durationMinutes = min, student = student)

    private val today = LocalDate.parse("2026-11-13")
    private val all = listOf(
        lesson("2025-12-06", 60, "Giulia Bianchi"), // last season (Dec start)
        lesson("2026-02-10", 90, "giulia bianchi "), // last season, other spelling
        lesson("2026-11-10", 120, "Giulia Bianchi"), // this season? no: the season starts on 1 Dec
        lesson("2026-11-12", 45, "Marco Ferri"),
    )

    @Test fun matches_the_student_ignoring_case_and_spaces() {
        val s = computeStudent(all, studentKey("  GIULIA bianchi"), today, 12, rateCents = null)!!
        assertEquals(3, s.lessonCount)
        assertEquals(270L, s.totalMinutes)
        assertEquals("Giulia Bianchi", s.name) // the most recent spelling
        assertEquals(LocalDate.parse("2025-12-06"), s.firstLesson)
        assertEquals(LocalDate.parse("2026-11-10"), s.lastLesson)
    }

    @Test fun average_rounds_half_up_and_earnings_follow_the_rate() {
        val s = computeStudent(all, "giulia bianchi", today, 12, rateCents = 3000)!!
        assertEquals(90L, s.averageMinutes)
        assertEquals(13500L, s.earningsCents) // 4h 30m at 30.00 €
        val odd = computeStudent(listOf(lesson("2026-11-01", 60, "A"), lesson("2026-11-02", 61, "a")), "a", today, 12, null)!!
        assertEquals(61L, odd.averageMinutes) // 60.5 rounds up
    }

    @Test fun seasons_split_at_the_start_month() {
        val s = computeStudent(all, "giulia bianchi", today, 12, null)!!
        assertEquals(2025, s.season.startYear) // 13 Nov 2026 is still in the 2025/26 season
        assertEquals(270L, s.thisSeasonMinutes)
        assertEquals(0L, s.lastSeasonMinutes)
        val novStart = computeStudent(all, "giulia bianchi", today, 11, null)!!
        assertEquals(120L, novStart.thisSeasonMinutes)
        assertEquals(150L, novStart.lastSeasonMinutes)
    }

    @Test fun last_six_months_and_months_newest_first() {
        val s = computeStudent(all, "giulia bianchi", today, 12, null)!!
        assertEquals(6, s.lastSixMonths.size)
        assertEquals(YearMonth.of(2026, 11), s.lastSixMonths.last().month)
        assertEquals(120L, s.lastSixMonths.last().minutes)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 2), YearMonth.of(2025, 12)), s.months.map { it.month })
    }

    @Test fun no_lessons_no_page() {
        assertNull(computeStudent(all, "nobody", today, 12, null))
    }
}
