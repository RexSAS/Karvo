package com.karvo.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class NoteRemindersTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun at(s: String) = ZonedDateTime.of(java.time.LocalDateTime.parse(s), rome)
    private val notes = mapOf(LocalDate.parse("2026-11-08") to "Today", LocalDate.parse("2026-11-12") to "Later", LocalDate.parse("2026-11-01") to "Past")

    @Test fun `today's note before 07-30 rings today at 07-30`() =
        assertEquals(at("2026-11-08T07:30").toInstant(), nextReminder(notes, at("2026-11-08T06:00"))?.second)

    @Test fun `after 07-30 the next note day rings`() {
        val r = nextReminder(notes, at("2026-11-08T07:31"))!!
        assertEquals(LocalDate.parse("2026-11-12"), r.first)
        assertEquals(at("2026-11-12T07:30").toInstant(), r.second)
    }

    @Test fun `no future notes, no alarm`() = assertNull(nextReminder(notes, at("2026-11-13T08:00")))

    @Test fun `07-30 follows daylight saving changes`() {
        val dst = mapOf(LocalDate.parse("2027-03-28") to "DST day")
        assertEquals(ZonedDateTime.of(2027, 3, 28, 7, 30, 0, 0, rome).toInstant(), nextReminder(dst, at("2027-03-27T12:00"))?.second)
    }
}
