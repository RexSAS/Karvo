package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate

/** What an import will do: the lessons to add, and how many file rows were skipped as duplicates. */
data class ImportPlan(val toAdd: List<Lesson>, val skipped: Int)

private data class LessonKey(val date: LocalDate, val minutes: Int, val student: String)

private fun Lesson.key() = LessonKey(date, durationMinutes, student.trim().lowercase())

/**
 * A file lesson is a duplicate when the app (or an earlier row of the same file) already has a lesson on
 * the same date, with the same duration and student (ignoring case and surrounding spaces). Notes and
 * paid dates are not compared.
 */
fun planImport(existing: List<Lesson>, incoming: List<Lesson>): ImportPlan {
    val seen = existing.mapTo(HashSet()) { it.key() }
    val toAdd = incoming.filter { seen.add(it.key()) }
    return ImportPlan(toAdd, incoming.size - toAdd.size)
}
