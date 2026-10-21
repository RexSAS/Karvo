package com.karvo.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/** Behavior every [LessonRepository] must share; run against the Room impl and the test fake. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
abstract class LessonRepositoryContract {
    abstract fun repo(): LessonRepository

    protected val d: LocalDate = LocalDate.of(2026, 10, 20)
    private fun lesson(date: LocalDate = d, student: String = "A", minutes: Int = 60, notes: String? = null) =
        Lesson(date = date, durationMinutes = minutes, student = student, notes = notes)

    @Test fun editUpdatesNotDuplicates() = runTest {
        val r = repo()
        val id = r.save(lesson(student = "Anna"))
        r.save(r.get(id)!!.copy(durationMinutes = 90))
        val all = r.allLessons(); assertEquals(1, all.size); assertEquals(90, all[0].durationMinutes)
    }

    @Test fun idsAssignedIncrementallyFromOne() = runTest {
        val r = repo()
        assertEquals(1L, r.save(lesson()))
        assertEquals(2L, r.save(lesson()))
    }

    @Test fun restoreKeepsId() = runTest {
        val r = repo()
        val id = r.save(lesson(student = "Anna"))
        val l = r.get(id)!!; r.delete(l); r.restore(l)
        assertEquals(l, r.get(id))
    }

    @Test fun normalizes() = runTest {
        val r = repo()
        val id = r.save(lesson(student = "  Anna ", notes = "   "))
        val l = r.get(id)!!; assertEquals("Anna", l.student); assertNull(l.notes)
    }

    @Test fun trimsNotes() = runTest {
        val r = repo()
        assertEquals("hi", r.get(r.save(lesson(notes = "  hi ")))!!.notes)
    }

    @Test fun acceptsDurationBounds() = runTest {
        val r = repo()
        r.save(lesson(minutes = 1)); r.save(lesson(minutes = 1440))
        assertEquals(2, r.allLessons().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroDuration() = runTest { repo().save(lesson(minutes = 0)) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverMaxDuration() = runTest { repo().save(lesson(minutes = 1441)) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankStudent() = runTest { repo().save(lesson(student = "  ")) }

    @Test fun lessonsOrderedDateDescThenIdDesc() = runTest {
        val r = repo()
        r.save(lesson(date = LocalDate.of(2026, 3, 1), student = "first"))
        r.save(lesson(date = LocalDate.of(2026, 1, 1), student = "old"))
        r.save(lesson(date = LocalDate.of(2026, 3, 1), student = "second"))
        assertEquals(listOf("second", "first", "old"), r.observeLessons().first().map { it.student })
        assertEquals(listOf("second", "first", "old"), r.allLessons().map { it.student })
    }

    @Test fun studentNamesDistinctMostRecentFirst() = runTest {
        val r = repo()
        r.save(lesson(date = LocalDate.of(2026, 1, 1), student = "Anna"))
        r.save(lesson(date = LocalDate.of(2026, 2, 1), student = "Bob"))
        r.save(lesson(date = LocalDate.of(2026, 3, 1), student = "Anna"))
        assertEquals(listOf("Anna", "Bob"), r.observeStudentNames().first())
    }

    @Test fun studentNamesRankByMaxDateThenMaxIdIndependently() = runTest {
        val r = repo()
        r.save(lesson(date = LocalDate.of(2026, 3, 1), student = "Anna")) // id 1
        r.save(lesson(date = LocalDate.of(2026, 1, 1), student = "Zed"))  // id 2
        r.save(lesson(date = LocalDate.of(2026, 3, 1), student = "Bob"))  // id 3
        r.save(lesson(date = LocalDate.of(2026, 1, 1), student = "Anna")) // id 4
        assertEquals(listOf("Anna", "Bob", "Zed"), r.observeStudentNames().first())
    }

    private fun key(l: Lesson) = Triple(l.date, l.student, l.durationMinutes)

    @Test fun replaceAllSwapsEverything() = runTest {
        val r = repo()
        r.save(lesson(student = "Old1")); r.save(lesson(student = "Old2"))
        val fresh = listOf(lesson(student = "New1"), lesson(student = "New2", minutes = 90), lesson(student = "New3", notes = "n"))
        r.replaceAll(fresh)
        assertEquals(fresh.map(::key).toSet(), r.allLessons().map(::key).toSet())
        assertEquals(3, r.allLessons().size)
    }

    @Test fun replaceAllWithInvalidLessonChangesNothing() = runTest {
        val r = repo()
        r.save(lesson(student = "Old1")); r.save(lesson(student = "Old2"))
        val before = r.allLessons()
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { r.replaceAll(listOf(lesson(student = "Ok"), lesson(student = "  "))) }
        }
        assertEquals(before, r.allLessons())
    }

    @Test fun replaceAllWithEmptyListClears() = runTest {
        val r = repo()
        r.save(lesson())
        r.replaceAll(emptyList())
        assertEquals(emptyList<Lesson>(), r.allLessons())
    }

    @Test fun markMonthPaidOnlyTouchesUnpaidInMonth() = runTest {
        val r = repo()
        val a = r.save(lesson(date = LocalDate.of(2026, 12, 3), student = "A"))
        val b = r.save(lesson(date = LocalDate.of(2026, 12, 10), student = "B").copy(paidOn = LocalDate.of(2026, 12, 20)))
        val c = r.save(lesson(date = LocalDate.of(2027, 1, 2), student = "C"))
        r.markMonthPaid(YearMonth.of(2026, 12), LocalDate.of(2027, 1, 15))
        assertEquals(LocalDate.of(2027, 1, 15), r.get(a)!!.paidOn)
        assertEquals(LocalDate.of(2026, 12, 20), r.get(b)!!.paidOn)
        assertNull(r.get(c)!!.paidOn)
    }

    @Test fun markMonthUnpaidClearsOnlyThatMonth() = runTest {
        val r = repo()
        val paid = LocalDate.of(2027, 2, 1)
        val dec = r.save(lesson(date = LocalDate.of(2026, 12, 31)).copy(paidOn = paid))
        val jan = r.save(lesson(date = LocalDate.of(2027, 1, 1)).copy(paidOn = paid))
        r.markMonthUnpaid(YearMonth.of(2026, 12))
        assertNull(r.get(dec)!!.paidOn)
        assertEquals(paid, r.get(jan)!!.paidOn)
    }

    @Test fun addAllAssignsNewIdsAndKeepsPaidOn() = runTest {
        val r = repo()
        r.save(lesson(student = "Existing"))
        val paid = LocalDate.of(2027, 1, 15)
        r.addAll(listOf(lesson(student = " Imported ").copy(id = 7, paidOn = paid)))
        val imported = r.allLessons().single { it.student == "Imported" }
        assertEquals(paid, imported.paidOn)
        assertEquals(2, r.allLessons().size)
        assertEquals(2, r.allLessons().map { it.id }.toSet().size)
    }

    @Test fun addAllRejectsInvalidAndAddsNothing() = runTest {
        val r = repo()
        r.save(lesson(student = "Existing"))
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { r.addAll(listOf(lesson(student = "Ok"), lesson(student = "  "))) }
        }
        assertEquals(listOf("Existing"), r.allLessons().map { it.student })
    }
}
