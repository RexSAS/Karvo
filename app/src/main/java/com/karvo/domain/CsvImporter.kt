package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val HEADER = "date,student,duration_minutes,duration_hours,notes"

/** A backup/export file that can't be read; [line] is the 1-based line where the bad record starts. */
class BackupFormatException(val line: Int, message: String) : Exception("Line $line: $message")

/** One CSV record and the line it starts on. */
private class Record(val line: Int, val fields: List<String>)

/**
 * Reads lessons from a Karvo CSV (as written by [lessonsToCsv], optionally with a UTF-8 BOM).
 * All-or-nothing: any invalid record throws [BackupFormatException]. Returned lessons have id 0.
 */
fun parseLessonsCsv(text: String): List<Lesson> {
    val records = tokenize(text.removePrefix("﻿"))
    val header = records.firstOrNull()
    if (header == null || header.fields.joinToString(",") != HEADER) throw BackupFormatException(1, "Not a Karvo file")
    return records.drop(1).map(::toLesson)
}

private fun toLesson(r: Record): Lesson {
    if (r.fields.size != 5) throw BackupFormatException(r.line, "Expected 5 fields, found ${r.fields.size}")
    val (dateText, studentText, minutesText, _, notesText) = r.fields
    val date = try {
        LocalDate.parse(dateText)
    } catch (_: DateTimeParseException) {
        throw BackupFormatException(r.line, "Invalid date \"$dateText\"")
    }
    val student = studentText.trim()
    if (student.isEmpty()) throw BackupFormatException(r.line, "Missing student")
    val minutes = minutesText.toIntOrNull()
    if (minutes == null || minutes !in 1..MAX_DURATION_MINUTES) throw BackupFormatException(r.line, "Invalid duration \"$minutesText\"")
    return Lesson(date = date, durationMinutes = minutes, student = student, notes = notesText.trim().ifEmpty { null })
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
