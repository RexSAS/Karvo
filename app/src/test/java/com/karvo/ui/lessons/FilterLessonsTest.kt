package com.karvo.ui.lessons

import com.karvo.data.Lesson
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterLessonsTest {
    private val day = LocalDate.of(2026, 1, 10)
    private val anna = Lesson(id = 1, date = day, durationMinutes = 60, student = "Anna Rossi", notes = "parallel turns", paidOn = day)
    private val marco = Lesson(id = 2, date = day, durationMinutes = 60, student = "Marco")
    private val all = listOf(anna, marco)

    private fun ids(q: String, paid: Boolean? = null) = filterLessons(all, q, paid).map { it.id }

    @Test fun emptyQueryKeepsAll() = assertEquals(listOf(1L, 2L), ids("  "))
    @Test fun matchesStudentIgnoringCase() = assertEquals(listOf(2L), ids("marc"))
    @Test fun matchesNotes() = assertEquals(listOf(1L), ids("PARALLEL"))
    @Test fun paidOnly() = assertEquals(listOf(1L), ids("", paid = true))
    @Test fun notPaidOnly() = assertEquals(listOf(2L), ids("", paid = false))
    @Test fun queryAndFilterCombine() = assertEquals(emptyList<Long>(), ids("anna", paid = false))
}
