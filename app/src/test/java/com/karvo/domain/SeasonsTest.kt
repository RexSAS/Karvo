package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeasonsTest {
    private fun l(y: Int, m: Int, d: Int, min: Int = 60) = Lesson(date = LocalDate.of(y, m, d), durationMinutes = min, student = "A")

    @Test fun seasonOfNovemberStart() {
        assertEquals(Season(2026, 11), seasonOf(LocalDate.of(2026, 11, 1), 11))
        assertEquals(Season(2026, 11), seasonOf(LocalDate.of(2027, 10, 31), 11))
        assertEquals(Season(2025, 11), seasonOf(LocalDate.of(2026, 10, 31), 11))
    }

    @Test fun seasonOfEveryStartMonth() {
        for (m in 1..12) {
            val first = LocalDate.of(2026, m, 1)
            assertEquals("start $m", Season(2026, m), seasonOf(first, m))
            assertEquals("start $m", Season(2025, m), seasonOf(first.minusDays(1), m))
        }
    }

    @Test fun rangeCoversAYear() {
        val r = Season(2026, 11).range()
        assertEquals(LocalDate.of(2026, 11, 1), r.start)
        assertEquals(LocalDate.of(2027, 10, 31), r.endInclusive)
        assertEquals(LocalDate.of(2026, 12, 31), Season(2026, 1).range().endInclusive)
    }

    @Test fun labels() {
        assertEquals("2026/27", Season(2026, 11).label())
        assertEquals("2026", Season(2026, 1).label())
        assertEquals("2099/00", Season(2099, 12).label())
    }

    @Test fun currentSeasonCanBeEmpty() {
        val s = computeSeasons(listOf(l(2025, 12, 5)), LocalDate.of(2027, 1, 10), 11, null)
        assertEquals(Season(2026, 11), s.current.season)
        assertEquals(0L, s.current.minutes)
        assertEquals(0, s.current.lessonCount)
        assertEquals(listOf(Season(2025, 11)), s.all.map { it.season })
    }

    @Test fun allNewestFirstOnlyWithLessons() {
        val s = computeSeasons(
            listOf(l(2024, 12, 1, 30), l(2026, 12, 1, 90), l(2027, 1, 2, 60)),
            LocalDate.of(2027, 1, 10), 11, 2000,
        )
        assertEquals(listOf(Season(2026, 11), Season(2024, 11)), s.all.map { it.season })
        assertEquals(SeasonTotal(Season(2026, 11), 150, earningsCents(150, 2000), 2), s.all[0])
        assertEquals(s.all[0], s.current)
    }

    @Test fun previousToDateCountsUpToSameDay() {
        val s = computeSeasons(listOf(l(2025, 12, 5), l(2026, 1, 10, 30), l(2026, 1, 20)), LocalDate.of(2027, 1, 10), 11, null)
        assertEquals(90L, s.previousToDateMinutes) // 5 Dec + 10 Jan (inclusive), not 20 Jan
    }

    @Test fun previousNullWithoutLessons() =
        assertNull(computeSeasons(listOf(l(2026, 12, 1)), LocalDate.of(2027, 1, 10), 11, null).previousToDateMinutes)

    @Test fun leapDay() {
        val s = computeSeasons(listOf(l(2027, 2, 28), l(2027, 3, 1)), LocalDate.of(2028, 2, 29), 11, null)
        assertEquals(60L, s.previousToDateMinutes)
    }

    @Test fun earningsNullWithoutRate() =
        assertNull(computeSeasons(listOf(l(2026, 12, 1)), LocalDate.of(2027, 1, 10), 11, null).current.earningsCents)
}
