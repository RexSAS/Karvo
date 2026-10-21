package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PaidTest {
    private val paid = LocalDate.of(2027, 1, 15)
    private fun l(date: LocalDate, min: Int = 60, paidOn: LocalDate? = null) =
        Lesson(date = date, durationMinutes = min, student = "A", paidOn = paidOn)

    @Test fun statusAllNoneSome() {
        val d = LocalDate.of(2026, 12, 3)
        assertEquals(PaidStatus.PAID, paidStatus(listOf(l(d, paidOn = paid), l(d, paidOn = paid))))
        assertEquals(PaidStatus.UNPAID, paidStatus(listOf(l(d), l(d))))
        assertEquals(PaidStatus.PARTLY_PAID, paidStatus(listOf(l(d, paidOn = paid), l(d))))
        assertEquals(PaidStatus.UNPAID, paidStatus(emptyList()))
    }

    @Test fun unpaidExcludesFutureAndPaid() {
        val lessons = listOf(
            l(LocalDate.of(2026, 12, 3), 60),
            l(LocalDate.of(2027, 1, 5), 30),
            l(LocalDate.of(2027, 1, 20), 45), // after today: not owed yet
            l(LocalDate.of(2026, 11, 3), 120, paidOn = paid),
        )
        assertEquals(Unpaid(months = 2, minutes = 90, earningsCents = null), unpaidTotals(lessons, LocalDate.of(2027, 1, 10), null))
    }

    @Test fun unpaidIncludesToday() =
        assertEquals(60L, unpaidTotals(listOf(l(LocalDate.of(2027, 1, 10))), LocalDate.of(2027, 1, 10), null).minutes)

    @Test fun unpaidEarningsFromSummedMinutes() {
        val lessons = listOf(l(LocalDate.of(2026, 12, 3), 45), l(LocalDate.of(2026, 12, 4), 45))
        assertEquals(earningsCents(90, 2550), unpaidTotals(lessons, LocalDate.of(2027, 1, 10), 2550).earningsCents)
    }

    @Test fun nothingUnpaid() = assertEquals(Unpaid(0, 0, 0), unpaidTotals(emptyList(), LocalDate.of(2027, 1, 10), 2550))
}
