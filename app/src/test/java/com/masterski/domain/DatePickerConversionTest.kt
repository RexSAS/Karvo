package com.masterski.domain

import java.time.LocalDate
import java.util.TimeZone
import org.junit.Test
import org.junit.Assert.assertEquals

class DatePickerConversionTest {
    @Test fun roundTripInRome() {
        val old = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"))
        try {
            val d = LocalDate.of(2026, 10, 20)
            assertEquals(d, pickerMillisToLocalDate(d.toPickerMillis()))
            assertEquals(1792454400000L, d.toPickerMillis()) // 2026-10-20T00:00Z
        } finally { TimeZone.setDefault(old) }
    }
}
