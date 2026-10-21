package com.masterski.domain

import com.masterski.data.Lesson
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {
    private val today = LocalDate.of(2027, 2, 10)
    private fun l(date: String, min: Int, student: String) =
        Lesson(date = LocalDate.parse(date), durationMinutes = min, student = student)

    @Test fun emptyListGivesZeroes() {
        val s = computeStats(emptyList(), today, null)
        assertEquals(0, s.lessonsThisMonth)
        assertEquals(0L, s.averageLessonMinutes)
        assertEquals(0, s.studentCount)
        assertEquals((5 downTo 0).map { YearMonth.of(2027, 2).minusMonths(it.toLong()) }, s.lastSixMonths.map { it.month })
        assertTrue(s.lastSixMonths.all { it.minutes == 0L })
        assertTrue(s.topStudents.isEmpty())
        assertEquals(List(7) { 0 }, s.weekdayCounts)
    }

    @Test fun sixMonthsCrossYearAndZeroFill() {
        val s = computeStats(listOf(l("2026-12-05", 60, "A"), l("2027-02-01", 90, "A"), l("2026-08-01", 30, "A")), today, null)
        assertEquals(listOf(0L, 0L, 0L, 60L, 0L, 90L), s.lastSixMonths.map { it.minutes })
        assertEquals(YearMonth.of(2026, 9), s.lastSixMonths.first().month)
        assertEquals(YearMonth.of(2027, 2), s.lastSixMonths.last().month)
    }

    @Test fun studentsMergedIgnoringCaseAndSpaces() {
        val s = computeStats(
            listOf(l("2026-12-01", 60, "rossi"), l("2027-01-01", 30, " Rossi "), l("2027-01-02", 120, "Bianchi")),
            today, null,
        )
        assertEquals(2, s.studentCount)
        assertEquals(listOf(StudentStat("Bianchi", 120, null), StudentStat("Rossi", 90, null)), s.topStudents)
    }

    @Test fun topFiveCutoffAndTieBreakByName() {
        val lessons = listOf(
            l("2027-01-01", 100, "C"), l("2027-01-01", 100, "B"), l("2027-01-01", 90, "A"),
            l("2027-01-01", 80, "D"), l("2027-01-01", 70, "E"), l("2027-01-01", 10, "F"),
        )
        val names = computeStats(lessons, today, null).topStudents.map { it.name }
        assertEquals(listOf("B", "C", "A", "D", "E"), names)
    }

    @Test fun averageRoundsHalfUp() {
        assertEquals(61L, computeStats(listOf(l("2027-01-01", 60, "A"), l("2027-01-02", 61, "A")), today, null).averageLessonMinutes)
    }

    @Test fun weekdayCountsMondayFirst() {
        val s = computeStats(listOf(l("2027-02-08", 60, "A"), l("2027-02-08", 60, "B"), l("2027-02-14", 60, "A")), today, null)
        assertEquals(listOf(2, 0, 0, 0, 0, 0, 1), s.weekdayCounts)
    }

    @Test fun lessonsThisMonthAndFutureLessonsCount() {
        val s = computeStats(listOf(l("2027-02-01", 60, "A"), l("2027-02-25", 60, "A"), l("2027-03-01", 60, "Z")), today, null)
        assertEquals(2, s.lessonsThisMonth)
        assertEquals(2, s.studentCount)
    }

    @Test fun earningsWithRate() {
        val s = computeStats(listOf(l("2027-02-03", 90, "A")), today, 2000)
        assertEquals(3000L, s.lastSixMonths.last().earningsCents)
        assertEquals(3000L, s.topStudents.single().earningsCents)
        assertEquals(3000L, s.sixMonthEarningsCents)
    }

    @Test fun noRateMeansNullEarnings() {
        val s = computeStats(listOf(l("2027-02-03", 90, "A")), today, null)
        assertTrue(s.lastSixMonths.all { it.earningsCents == null })
        assertNull(s.topStudents.single().earningsCents)
        assertNull(s.sixMonthEarningsCents)
    }

    @Test fun sixMonthEarningsComputedFromSummedMinutes() {
        // 1 min at 1.00/h is 1.67 → 2 cents per month, but 3 min total is 5 cents.
        val s = computeStats(listOf(l("2026-12-01", 1, "A"), l("2027-01-01", 1, "A"), l("2027-02-01", 1, "A")), today, 100)
        assertEquals(5L, s.sixMonthEarningsCents)
    }
}
