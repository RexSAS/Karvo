package com.masterski.domain

import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthFormatTest {
    @Test fun italianMonthTitleIsCapitalised() =
        assertEquals("Ottobre 2026", monthTitle(YearMonth.of(2026, 10), Locale.ITALIAN))

    @Test fun englishMonthTitle() =
        assertEquals("October 2026", monthTitle(YearMonth.of(2026, 10), Locale.ENGLISH))

    @Test fun shortLabels() {
        assertEquals("Ott", shortMonthLabel(YearMonth.of(2026, 10), Locale.ITALIAN))
        assertEquals("Oct", shortMonthLabel(YearMonth.of(2026, 10), Locale.ENGLISH))
    }

    @Test fun dayTitles() {
        assertEquals("Wednesday 21 October", dayTitle(java.time.LocalDate.of(2026, 10, 21), Locale.ENGLISH))
        assertEquals("Mercoledì 21 ottobre", dayTitle(java.time.LocalDate.of(2026, 10, 21), Locale.ITALIAN))
    }
}
