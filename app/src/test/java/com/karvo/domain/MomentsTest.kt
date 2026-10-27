package com.karvo.domain

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MomentsTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun moment(at: String, video: Boolean = false) = Moment(Uri.parse("content://m/$at"), video, Instant.parse(at))

    @Test fun groupsByLocalDayNewestFirst() {
        // 23:30 UTC on the 1st is already the 2nd in Rome.
        val late = moment("2026-01-01T23:30:00Z")
        val morning = moment("2026-01-02T08:00:00Z", video = true)
        val earlier = moment("2025-12-30T10:00:00Z")
        val days = groupMomentsByDay(listOf(earlier, late, morning), rome)
        assertEquals(listOf(LocalDate.of(2026, 1, 2), LocalDate.of(2025, 12, 30)), days.map { it.date })
        assertEquals(listOf(morning, late), days[0].moments)
    }

    @Test fun countsPerMonth() {
        val counts = momentCountsByMonth(
            listOf(moment("2026-01-05T10:00:00Z"), moment("2026-01-20T10:00:00Z"), moment("2026-02-01T10:00:00Z")),
            rome,
        )
        assertEquals(mapOf(YearMonth.of(2026, 1) to 2, YearMonth.of(2026, 2) to 1), counts)
    }

    @Test fun momentsOnADayOldestFirst() {
        val b = moment("2026-01-02T10:00:00Z")
        val a = moment("2026-01-02T08:00:00Z")
        assertEquals(listOf(a, b), momentsOn(LocalDate.of(2026, 1, 2), listOf(b, moment("2026-01-03T08:00:00Z"), a), rome))
    }

    @Test fun calendarStartsOnMondayAndFillsWeeks() {
        // October 2026 starts on a Thursday and has 31 days.
        val cells = calendarCells(YearMonth.of(2026, 10))
        assertEquals(listOf(null, null, null), cells.take(3))
        assertEquals(LocalDate.of(2026, 10, 1), cells[3])
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2026, 10, 31), cells[33])
        assertEquals(null, cells[34])
    }
}
