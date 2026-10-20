package com.masterski.ui

import com.masterski.data.Lesson
import com.masterski.domain.LessonError
import com.masterski.testing.FakeLessonRepository
import com.masterski.testing.FakeSettingsRepository
import com.masterski.testing.MainDispatcherRule
import com.masterski.ui.edit.EditLessonViewModel
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
}
