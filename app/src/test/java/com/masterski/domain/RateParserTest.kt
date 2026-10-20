package com.masterski.domain

import org.junit.Test
import org.junit.Assert.assertEquals

class RateParserTest {
    @Test fun accepts() {
        assertEquals(RateParseResult.Valid(2550), parseRateCents("25,50"))
        assertEquals(RateParseResult.Valid(2550), parseRateCents("25.5"))
        assertEquals(RateParseResult.Valid(2500), parseRateCents(" 25 €"))
        assertEquals(RateParseResult.Valid(2500), parseRateCents("25"))
        assertEquals(RateParseResult.Valid(2500), parseRateCents("25,-"))
        assertEquals(RateParseResult.Valid(2500), parseRateCents("25.-"))
    }
    @Test fun normalizes() = assertEquals("25.50", normalizeRateInput("25,50"))
    @Test fun formatsRate() {
        assertEquals("25.00", formatRateText(2500)); assertEquals("25.50", formatRateText(2550)); assertEquals("0.05", formatRateText(5))
    }
    @Test fun cleared() = assertEquals(RateParseResult.Cleared, parseRateCents("  "))
    @Test fun rejects() = listOf("1.234,50", "-5", "25.555", "0", "abc", "25,", ",5x").forEach {
        assertEquals(it, RateParseResult.Invalid, parseRateCents(it))
    }
}
