package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate

/** What an import will do: the lessons to add, and how many file rows were skipped as duplicates. */
data class ImportPlan(val toAdd: List<Lesson>, val skipped: Int)

private data class LessonKey(val date: LocalDate, val minutes: Int, val student: String)

private fun Lesson.key() = LessonKey(date, durationMinutes, student.trim().lowercase())

/**
 * A file lesson is a duplicate when the app already has a lesson on the same date, with the same duration
 * and student (ignoring case and surrounding spaces). Matching counts copies: if the app has one such lesson
 * and the file has two, one is added — two identical lessons on a day (morning and afternoon) are both real.
 * Notes and paid dates are not compared.
 */
fun planImport(existing: List<Lesson>, incoming: List<Lesson>): ImportPlan {
    val inApp = existing.groupingBy { it.key() }.eachCount().toMutableMap()
    val toAdd = incoming.filter { lesson ->
        val left = inApp[lesson.key()] ?: 0
        if (left > 0) inApp[lesson.key()] = left - 1
        left == 0
    }
    return ImportPlan(toAdd, incoming.size - toAdd.size)
}
