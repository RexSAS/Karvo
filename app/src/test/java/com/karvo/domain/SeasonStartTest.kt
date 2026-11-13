package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SeasonStartTest {
    private fun lesson(date: String, min: Int, student: String, paid: String? = null) =
        Lesson(date = LocalDate.parse(date), durationMinutes = min, student = student, paidOn = paid?.let(LocalDate::parse))

    // Last season 2025/26 (December start): two months, January only partly paid; one lesson this season.
    private val lessons = listOf(
        lesson("2025-12-06", 60, "Giulia", paid = "2026-01-10"),
        lesson("2026-01-10", 90, "giulia "),
        lesson("2026-01-11", 120, "Marco", paid = "2026-02-10"),
        lesson("2026-12-02", 60, "Elena"),
    )
    private val firstDay = LocalDate.parse("2026-12-01")

    @Test fun reminder_days_are_this_and_next_years_first_days() {
        assertEquals(listOf(LocalDate.parse("2026-12-01"), LocalDate.parse("2027-12-01")), seasonStartDays(LocalDate.parse("2026-11-13"), 12))
    }

    @Test fun a_new_season_gets_the_card_with_last_season_in_it() {
        val card = seasonWelcome(lessons, firstDay, 12, welcomedSeason = 2025, rateCents = 3000)!!
        assertEquals("2026/27", card.season.label())
        assertEquals(270L, card.last!!.minutes)
        assertEquals(3, card.last!!.lessonCount)
        assertEquals(13500L, card.last!!.earningsCents)
        assertEquals(2, card.lastStudents) // Giulia twice (case and spaces ignored), Marco
        assertEquals(1, card.lastUnpaidMonths) // January: one lesson still unpaid
    }

    @Test fun no_card_twice_in_a_season_or_before_it_ever_was_stored() {
        assertNull(seasonWelcome(lessons, firstDay, 12, welcomedSeason = 2026, rateCents = null))
        assertNull(seasonWelcome(lessons, firstDay, 12, welcomedSeason = null, rateCents = null)) // fresh install or update
        assertNull(seasonWelcome(emptyList(), firstDay, 12, welcomedSeason = 2025, rateCents = null))
    }

    @Test fun an_empty_last_season_still_welcomes_without_a_summary() {
        val card = seasonWelcome(listOf(lesson("2024-01-05", 60, "A")), firstDay, 12, welcomedSeason = 2024, rateCents = null)
        assertNotNull(card)
        assertNull(card!!.last)
        assertEquals(0, card.lastUnpaidMonths)
    }
}
