package com.karvo.domain

import com.karvo.data.Lesson
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private const val CRLF = "\r\n"

/** Header written by [lessonsToCsv]; files from before 2.1 lack the last column (see [parseLessonsCsv]). */
const val CSV_HEADER = "date,student,duration_minutes,duration_hours,notes,paid_on"

fun durationHoursText(minutes: Int): String =
    BigDecimal(minutes).divide(BigDecimal(60), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

fun csvFileName(today: LocalDate): String = "karvo-lessons-$today.csv"

fun lessonsToCsv(lessons: List<Lesson>): String = buildString {
    append(CSV_HEADER).append(CRLF)
    lessons.sortedWith(compareBy<Lesson> { it.date }.thenBy { it.id }).forEach {
        append(
            listOf(
                it.date.toString(), it.student, it.durationMinutes.toString(),
                durationHoursText(it.durationMinutes), it.notes.orEmpty(), it.paidOn?.toString().orEmpty(),
            ).joinToString(",") { f -> csvField(f) },
        ).append(CRLF)
    }
}

private fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

/** Bytes written to the export file: UTF-8 BOM (so Excel detects UTF-8) followed by [csv]. */
fun csvFileBytes(csv: String): ByteArray = UTF8_BOM + csv.toByteArray(Charsets.UTF_8)
