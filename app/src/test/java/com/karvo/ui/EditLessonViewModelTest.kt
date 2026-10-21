package com.karvo.ui

import com.karvo.data.Lesson
import com.karvo.domain.LessonError
import com.karvo.testing.FakeLessonRepository
import com.karvo.testing.FakeSettingsRepository
import com.karvo.testing.GatedLessonRepository
import com.karvo.testing.MainDispatcherRule
import com.karvo.ui.edit.EditLessonViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditLessonViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val repo = FakeLessonRepository()

    private fun newVm() = EditLessonViewModel(repo, FakeSettingsRepository(), null)

    @Test fun defaultDateUsesConfiguredZone() = runTest {
        val clock = Clock.fixed(Instant.parse("2026-10-19T22:30:00Z"), ZoneOffset.UTC) // 00:30 in Rome
        val vm = EditLessonViewModel(repo, FakeSettingsRepository(zone = "Europe/Rome"), null, clock)
        assertEquals(LocalDate.of(2026, 10, 20), vm.uiState.value.date); assertFalse(vm.uiState.value.isValid)
        val ny = EditLessonViewModel(repo, FakeSettingsRepository(zone = "America/New_York"), null, clock)
        assertEquals(LocalDate.of(2026, 10, 19), ny.uiState.value.date)
    }
    @Test fun quickPickSetsFields() = runTest {
        val vm = newVm(); vm.onQuickPick(90)
        assertEquals("1", vm.uiState.value.hoursText); assertEquals("30", vm.uiState.value.minutesText)
    }
    @Test fun nonDigitsIgnored() = runTest {
        val vm = newVm(); vm.onHoursChange("2a"); assertEquals("", vm.uiState.value.hoursText)
        vm.onHoursChange("2"); assertEquals("2", vm.uiState.value.hoursText)
    }
    @Test fun saveNewInserts() = runTest {
        val vm = newVm(); vm.onStudentChange("Anna"); vm.onQuickPick(60)
        var done = false; vm.save { done = true }
        assertTrue(done); assertEquals(60, repo.allLessons().single().durationMinutes)
    }
    @Test fun editLoadsAndUpdates() = runTest {
        val id = repo.save(Lesson(date = LocalDate.of(2026, 1, 1), durationMinutes = 120, student = "Bob"))
        val vm = EditLessonViewModel(repo, FakeSettingsRepository(), id)
        assertEquals("Bob", vm.uiState.value.student); assertEquals("2", vm.uiState.value.hoursText); assertFalse(vm.uiState.value.isNew)
        vm.onStudentChange("Bobby"); vm.save {}
        assertEquals(listOf("Bobby"), repo.allLessons().map { it.student })
    }
    @Test fun invalidSaveDoesNothing() = runTest {
        val vm = newVm(); var done = false; vm.save { done = true }
        assertFalse(done); assertTrue(repo.allLessons().isEmpty())
    }
    @Test fun suggestionsFilterByPrefix() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 1, 1), durationMinutes = 60, student = "Anna"))
        repo.save(Lesson(date = LocalDate.of(2026, 1, 2), durationMinutes = 60, student = "Bob"))
        val vm = newVm(); vm.onStudentChange("an")
        assertEquals(listOf("Anna"), vm.uiState.value.suggestions)
    }
    @Test fun deleteRemovesEditedLesson() = runTest {
        val id = repo.save(Lesson(date = LocalDate.of(2026, 1, 1), durationMinutes = 60, student = "Bob"))
        val vm = EditLessonViewModel(repo, FakeSettingsRepository(), id)
        var done = false; vm.delete { done = true }
        assertTrue(done); assertTrue(repo.allLessons().isEmpty())
    }
    @Test fun errorsTrackInput() = runTest {
        val vm = newVm()
        assertEquals(setOf(LessonError.StudentBlank, LessonError.DurationZero), vm.uiState.value.errors)
        vm.onMinutesChange("75")
        assertTrue(LessonError.MinutesOver59 in vm.uiState.value.errors)
    }
    @Test fun doubleSaveInsertsOnce() = runTest {
        val gated = GatedLessonRepository(repo)
        val vm = EditLessonViewModel(gated, FakeSettingsRepository(), null)
        vm.onStudentChange("Anna"); vm.onQuickPick(60)
        var doneCount = 0
        vm.save { doneCount++ }; vm.save { doneCount++ }
        gated.gate.complete(Unit)
        assertEquals(1, repo.allLessons().size); assertEquals(1, doneCount)
    }
    @Test fun doubleDeleteCallsDoneOnce() = runTest {
        val id = repo.save(Lesson(date = LocalDate.of(2026, 1, 1), durationMinutes = 60, student = "Bob"))
        val gated = GatedLessonRepository(repo)
        val vm = EditLessonViewModel(gated, FakeSettingsRepository(), id)
        var doneCount = 0
        vm.delete { doneCount++ }; vm.delete { doneCount++ }
        gated.gate.complete(Unit)
        assertTrue(repo.allLessons().isEmpty()); assertEquals(1, doneCount)
    }

    @Test fun editKeepsPaidOn() = runTest {
        val paid = LocalDate.of(2027, 1, 15)
        val id = repo.save(Lesson(date = LocalDate.of(2026, 12, 3), durationMinutes = 60, student = "Rossi", paidOn = paid))
        val vm = EditLessonViewModel(repo, FakeSettingsRepository(), id)
        vm.onStudentChange("Rossi Anna"); vm.save {}
        assertEquals(paid, repo.get(id)!!.paidOn)
    }
    @Test fun editMovingDateKeepsPaidOn() = runTest {
        val paid = LocalDate.of(2027, 1, 15)
        val id = repo.save(Lesson(date = LocalDate.of(2026, 12, 3), durationMinutes = 60, student = "Rossi", paidOn = paid))
        val vm = EditLessonViewModel(repo, FakeSettingsRepository(), id)
        vm.onDateChange(LocalDate.of(2027, 2, 1)); vm.save {}
        assertEquals(LocalDate.of(2027, 2, 1), repo.get(id)!!.date)
        assertEquals(paid, repo.get(id)!!.paidOn)
    }
    @Test fun newLessonIsUnpaid() = runTest {
        val vm = newVm(); vm.onStudentChange("Anna"); vm.onQuickPick(60); vm.save {}
        assertEquals(null, repo.allLessons().single().paidOn)
    }
}
