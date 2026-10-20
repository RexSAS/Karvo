package com.masterski.domain

import java.util.Locale
import org.junit.Test
import org.junit.Assert.assertEquals

class FormatTest {
    @Test fun durations() {
        assertEquals("0m", formatDuration(0)); assertEquals("45m", formatDuration(45))
        assertEquals("2h", formatDuration(120)); assertEquals("1h 30m", formatDuration(90))
        assertEquals("142h 30m", formatDuration(8550))
    }
    @Test fun euroItalian() = assertEquals("1.234,50 €", formatEuro(123450, Locale.ITALY).replace(' ', ' '))
    @Test fun euroUs() = assertEquals("€1,234.50", formatEuro(123450, Locale.US))
    @Test fun labelHidden() = assertEquals("**** €", earningsLabel(5000, revealed = false))
    @Test fun labelNoRate() { assertEquals("— €", earningsLabel(null, false)); assertEquals("— €", earningsLabel(null, true)) }
    @Test fun labelRevealed() = assertEquals("€50.00", earningsLabel(5000, true, Locale.US))
}
