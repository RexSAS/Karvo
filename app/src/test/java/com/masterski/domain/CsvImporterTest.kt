package com.masterski.domain

import com.masterski.data.Lesson
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CsvImporterTest {
    private val header = "date,student,duration_minutes,duration_hours,notes"
    private val tricky = listOf(
        Lesson(date = LocalDate.of(2026, 1, 2), durationMinutes = 90, student = "Rossi, Mario", notes = "said \"hi\"\r\nline2"),
        Lesson(date = LocalDate.of(2026, 2, 3), durationMinutes = 60, student = "Zoë Ångström", notes = null),
        Lesson(date = LocalDate.of(2026, 3, 4), durationMinutes = 1440, student = "A", notes = "x,y"),
    )

    private fun lineOf(csv: String): Int = assertThrows(BackupFormatException::class.java) { parseLessonsCsv(csv) }.line

    @Test fun roundTripsTrickyValues() = assertEquals(tricky, parseLessonsCsv(lessonsToCsv(tricky)))

    @Test fun acceptsBomAndLf() {
        val csv = lessonsToCsv(tricky)
        assertEquals(tricky, parseLessonsCsv(String(csvFileBytes(csv), Charsets.UTF_8)))
        // Lessons without line breaks in their fields, written with LF-only line endings.
        val plain = tricky.drop(1)
        assertEquals(plain, parseLessonsCsv(lessonsToCsv(plain).replace("\r\n", "\n")))
    }

    @Test fun headerOnlyGivesEmptyList() = assertEquals(emptyList<Lesson>(), parseLessonsCsv("$header\r\n"))

    @Test fun rejectsWrongHeader() = assertEquals(1, lineOf("date,student\r\n"))

    @Test fun rejectsWrongFieldCount() =
        assertEquals(3, lineOf("$header\r\n2026-01-01,A,60,1,\r\n2026-01-02,A,60,1\r\n"))

    @Test fun rejectsBadDate() = assertEquals(2, lineOf("$header\r\n2026-13-01,A,60,1,\r\n"))

    @Test fun rejectsBlankStudent() = assertEquals(2, lineOf("$header\r\n2026-01-01,  ,60,1,\r\n"))

    @Test fun rejectsBadMinutes() {
        assertEquals(2, lineOf("$header\r\n2026-01-01,A,0,0,\r\n"))
        assertEquals(2, lineOf("$header\r\n2026-01-01,A,1441,24,\r\n"))
        assertEquals(2, lineOf("$header\r\n2026-01-01,A,abc,1,\r\n"))
    }

    @Test fun rejectsUnterminatedQuote() = assertEquals(2, lineOf("$header\r\n2026-01-01,A,60,1,\"open\r\n"))

    @Test fun lineNumbersCountLineBreaksInsideQuotes() =
        assertEquals(4, lineOf("$header\r\n2026-01-01,A,60,1,\"a\r\nb\"\r\n2026-01-01,,60,1,\r\n"))
}
