package com.karvo.domain

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

    @Test fun chartLabelKeepsMinutesBelow100Hours() {
        assertEquals("99h 30m", chartDurationLabel(99 * 60 + 30))
        assertEquals("45m", chartDurationLabel(45))
    }

    @Test fun chartLabelDropsMinutesFrom100Hours() {
        // "152h 30m" does not fit a sixth of a phone-width chart; whole hours, rounded half-up, do.
        assertEquals("153h", chartDurationLabel(152 * 60 + 30))
        assertEquals("100h", chartDurationLabel(100 * 60))
    }
}
