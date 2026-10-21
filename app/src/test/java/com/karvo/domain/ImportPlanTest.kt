package com.karvo.domain

import com.karvo.data.Lesson
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportPlanTest {
    private val day = LocalDate.of(2026, 12, 3)
    private fun lesson(student: String = "Rossi", minutes: Int = 60, date: LocalDate = day, notes: String? = null, paidOn: LocalDate? = null) =
        Lesson(date = date, durationMinutes = minutes, student = student, notes = notes, paidOn = paidOn)

    @Test fun skipsLessonAlreadyInApp() {
        val plan = planImport(existing = listOf(lesson("Rossi").copy(id = 1)), incoming = listOf(lesson(" rossi ")))
        assertEquals(emptyList<Lesson>(), plan.toAdd)
        assertEquals(1, plan.skipped)
    }

    @Test fun differentDurationIsNew() {
        val plan = planImport(listOf(lesson(minutes = 60)), listOf(lesson(minutes = 90)))
        assertEquals(listOf(lesson(minutes = 90)), plan.toAdd)
        assertEquals(0, plan.skipped)
    }

    @Test fun differentDateIsNew() =
        assertEquals(1, planImport(listOf(lesson()), listOf(lesson(date = day.plusDays(1)))).toAdd.size)

    @Test fun notesAndPaidOnIgnoredForMatch() {
        val plan = planImport(listOf(lesson(notes = "a")), listOf(lesson(notes = "b", paidOn = day)))
        assertEquals(1, plan.skipped)
    }

    @Test fun duplicateRowsInsideFileCountOnce() {
        val plan = planImport(emptyList(), listOf(lesson(), lesson("ROSSI")))
        assertEquals(listOf(lesson()), plan.toAdd)
        assertEquals(1, plan.skipped)
    }

    @Test fun keepsIncomingOrderAndPaidOn() {
        val a = lesson("A", paidOn = LocalDate.of(2027, 1, 15))
        val b = lesson("B")
        assertEquals(listOf(a, b), planImport(emptyList(), listOf(a, b)).toAdd)
    }
}
