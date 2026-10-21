package com.karvo.domain

import org.junit.Test
import org.junit.Assert.assertEquals

class EarningsTest {
    @Test fun wholeHours() = assertEquals(5100, earningsCents(120, 2550))
    @Test fun roundsHalfUp() = assertEquals(1, earningsCents(1, 30))
    @Test fun roundsDown() = assertEquals(0, earningsCents(1, 29))
    @Test fun fromSummedMinutes() = assertEquals(1000, earningsCents(20 + 20 + 20, 1000))
}
