package com.masterski.ui

import com.masterski.data.Lesson
import com.masterski.testing.FakeLessonRepository
import com.masterski.testing.MainDispatcherRule
import com.masterski.ui.lessons.LessonsViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LessonsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    private val repo = FakeLessonRepository()

    @Test fun groupsByMonthNewestFirst() = runTest {
        repo.save(Lesson(date = LocalDate.of(2026, 9, 1), durationMinutes = 60, student = "A"))
        repo.save(Lesson(date = LocalDate.of(2026, 10, 5), durationMinutes = 60, student = "B"))
        val vm = LessonsViewModel(repo)
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9)), vm.groups.first { it.isNotEmpty() }.map { it.month })
    }

    @Test fun undoRestoresSameLesson() = runTest {
        val id = repo.save(Lesson(date = LocalDate.of(2026, 10, 5), durationMinutes = 60, student = "B"))
        val vm = LessonsViewModel(repo); val l = repo.get(id)!!
        vm.delete(l); assertTrue(repo.allLessons().isEmpty())
        vm.undoDelete(l); assertEquals(listOf(l), repo.allLessons())
    }
}
