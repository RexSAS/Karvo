package com.masterski.domain

import com.masterski.data.Lesson
import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class CsvExporterTest {
    @Test fun headerAndRow() {
        val csv = lessonsToCsv(listOf(Lesson(1, LocalDate.of(2026, 10, 20), 90, "Anna", null)))
        assertEquals("date,student,duration_minutes,duration_hours,notes\r\n2026-10-20,Anna,90,1.5,\r\n", csv)
    }

    @Test fun escapes() {
        val csv = lessonsToCsv(listOf(Lesson(1, LocalDate.of(2026, 1, 2), 60, "Group \"Rossi\", kids", "line1\nline2")))
        assertTrue(csv.endsWith("2026-01-02,\"Group \"\"Rossi\"\", kids\",60,1,\"line1\nline2\"\r\n"))
    }

    @Test fun sortedOldestFirst() {
        val csv = lessonsToCsv(listOf(Lesson(2, LocalDate.of(2026, 2, 1), 60, "B"), Lesson(1, LocalDate.of(2026, 1, 1), 60, "A")))
        assertTrue(csv.indexOf("2026-01-01") < csv.indexOf("2026-02-01"))
    }

    @Test fun hoursText() {
        assertEquals("1", durationHoursText(60)); assertEquals("0.83", durationHoursText(50)); assertEquals("2.25", durationHoursText(135))
    }

    @Test fun fileName() = assertEquals("masterski-lessons-2026-10-20.csv", csvFileName(LocalDate.of(2026, 10, 20)))
    @Test fun fileBytesStartWithUtf8Bom() {
        val bytes = csvFileBytes("a,b\r\n")
        assertEquals(listOf(0xEF, 0xBB, 0xBF), bytes.take(3).map { it.toInt() and 0xFF })
        assertEquals("a,b\r\n", String(bytes.copyOfRange(3, bytes.size), Charsets.UTF_8))
    }
}
