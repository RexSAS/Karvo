package com.karvo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesTest {
    @Test fun noHoursNoBadge() {
        val p = badgeProgress(0)
        assertNull(p.earned)
        assertEquals(Badge.KARVER, p.next)
        assertEquals(0f, p.fraction)
        assertEquals(60L, p.minutesToNext)
    }

    @Test fun firstHourIsKarver() {
        assertEquals(Badge.KARVER, badgeProgress(60).earned)
        assertNull(badgeProgress(59).earned)
    }

    @Test fun exactlyOnTheLineUnlocks() {
        val p = badgeProgress(50 * 60)
        assertEquals(Badge.PARALLEL, p.earned)
        assertEquals(Badge.RED_RUN, p.next)
        assertEquals(0f, p.fraction)
    }

    @Test fun halfwayBetweenBadges() {
        val p = badgeProgress(125 * 60) // Edge master 100h, Black run 150h
        assertEquals(0.5f, p.fraction)
        assertEquals(25L * 60, p.minutesToNext)
    }

    @Test fun allEarned() {
        val p = badgeProgress(1000 * 60)
        assertEquals(Badge.LEGEND, p.earned)
        assertNull(p.next)
        assertEquals(1f, p.fraction)
        assertEquals(0L, p.minutesToNext)
    }

    @Test fun laddersGoUpAndTiersHoldThree() {
        assertTrue(Badge.entries.zipWithNext().all { (a, b) -> a.hours < b.hours })
        assertEquals(listOf(0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4), Badge.entries.map { it.tier })
        assertEquals(listOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1), Badge.entries.map { it.step })
    }
}
