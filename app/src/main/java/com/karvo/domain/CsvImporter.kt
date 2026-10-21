package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Header of files written before 2.1, without paid_on. */
private const val HEADER_V1 = "date,student,duration_minutes,duration_hours,notes"

/** A backup/export file that can't be read; [line] is the 1-based line where the bad record starts. */
class BackupFormatException(val line: Int, message: String) : Exception("Line $line: $message")

/** One CSV record and the line it starts on. */
private class Record(val line: Int, val fields: List<String>)

/**
 * Reads lessons from a Karvo CSV (as written by [lessonsToCsv], optionally with a UTF-8 BOM). Files from
 * before 2.1 have no paid_on column; their lessons come back unpaid.
 * All-or-nothing: any invalid record throws [BackupFormatException]. Returned lessons have id 0.
 */
fun parseLessonsCsv(text: String): List<Lesson> {
    val records = tokenize(text.removePrefix("﻿"))
    val header = records.firstOrNull()
    val fieldCount = when (header?.fields?.joinToString(",")) {
        CSV_HEADER -> 6
        HEADER_V1 -> 5
        else -> throw BackupFormatException(1, "Not a Karvo file")
    }
    return records.drop(1).map { toLesson(it, fieldCount) }
}

private fun toLesson(r: Record, fieldCount: Int): Lesson {
    if (r.fields.size != fieldCount) throw BackupFormatException(r.line, "Expected $fieldCount fields, found ${r.fields.size}")
    val (dateText, studentText, minutesText, _, notesText) = r.fields
    val paidText = r.fields.getOrNull(5).orEmpty()
    val date = try {
        LocalDate.parse(dateText)
    } catch (_: DateTimeParseException) {
        throw BackupFormatException(r.line, "Invalid date \"$dateText\"")
    }
    val student = studentText.trim()
    if (student.isEmpty()) throw BackupFormatException(r.line, "Missing student")
    val minutes = minutesText.toIntOrNull()
    if (minutes == null || minutes !in 1..MAX_DURATION_MINUTES) throw BackupFormatException(r.line, "Invalid duration \"$minutesText\"")
    val paidOn = if (paidText.isEmpty()) {
        null
    } else {
        try {
            LocalDate.parse(paidText)
        } catch (_: DateTimeParseException) {
            throw BackupFormatException(r.line, "Invalid paid date \"$paidText\"")
        }
    }
    return Lesson(date = date, durationMinutes = minutes, student = student, notes = notesText.trim().ifEmpty { null }, paidOn = paidOn)
}

/** RFC 4180 tokenizer: quoted fields may hold commas, doubled quotes and line breaks; CRLF, CR or LF end a record. */
private fun tokenize(text: String): List<Record> {
    val records = mutableListOf<Record>()
    val fields = mutableListOf<String>()
    val field = StringBuilder()
    var line = 1
    var recordLine = 1
    var inQuotes = false
    var i = 0

    fun endRecord() {
        fields += field.toString(); field.clear()
        records += Record(recordLine, fields.toList()); fields.clear()
    }

    while (i < text.length) {
        val c = text[i]
        if (inQuotes) {
            when {
                c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = false
                else -> {
                    if (c == '\n' || (c == '\r' && text.getOrNull(i + 1) != '\n')) line++
                    field.append(c)
                }
            }
        } else {
            when (c) {
                '"' -> inQuotes = true
                ',' -> { fields += field.toString(); field.clear() }
                '\r', '\n' -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    endRecord()
                    line++
                    recordLine = line
                }
                else -> field.append(c)
            }
        }
        i++
    }
    if (inQuotes) throw BackupFormatException(recordLine, "Unterminated quoted field")
    if (field.isNotEmpty() || fields.isNotEmpty()) endRecord()
    return records
}
