package com.masterski.domain

import com.masterski.data.Lesson
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private const val CRLF = "\r\n"

fun durationHoursText(minutes: Int): String =
    BigDecimal(minutes).divide(BigDecimal(60), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

fun csvFileName(today: LocalDate): String = "masterski-lessons-$today.csv"

fun lessonsToCsv(lessons: List<Lesson>): String = buildString {
    append("date,student,duration_minutes,duration_hours,notes").append(CRLF)
    lessons.sortedWith(compareBy<Lesson> { it.date }.thenBy { it.id }).forEach {
        append(
            listOf(
                it.date.toString(), it.student, it.durationMinutes.toString(),
                durationHoursText(it.durationMinutes), it.notes.orEmpty(),
            ).joinToString(",") { f -> csvField(f) },
        ).append(CRLF)
    }
}

private fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

/** Bytes written to the export file: UTF-8 BOM (so Excel detects UTF-8) followed by [csv]. */
fun csvFileBytes(csv: String): ByteArray = UTF8_BOM + csv.toByteArray(Charsets.UTF_8)
