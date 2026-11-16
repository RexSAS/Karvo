package com.karvo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BadgesTest {
    @Test fun noHoursNoBadge() {
        val p = badgeProgress(0)
        assertNull(p.earned)
        assertEquals(Badge.FIRST_TRACKS, p.next)
        assertEquals(0f, p.fraction)
        assertEquals(600L, p.minutesToNext)
    }

    @Test fun exactlyOnTheLineUnlocks() {
        val p = badgeProgress(50 * 60)
        assertEquals(Badge.SNOWPLOUGH, p.earned)
        assertEquals(Badge.PARALLEL, p.next)
        assertEquals(0f, p.fraction)
    }

    @Test fun halfwayBetweenBadges() {
        val p = badgeProgress(75 * 60)
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
}
