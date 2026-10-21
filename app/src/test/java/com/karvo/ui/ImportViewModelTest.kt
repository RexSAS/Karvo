package com.karvo.ui

import com.karvo.data.Lesson
import com.karvo.data.LessonRepository
import com.karvo.data.backup.BackupReason
import com.karvo.data.backup.BackupService
import com.karvo.data.backup.BackupStore
import com.karvo.data.backup.TickingClock
import com.karvo.domain.lessonsToCsv
import com.karvo.testing.FakeLessonRepository
import com.karvo.testing.MainDispatcherRule
import com.karvo.ui.settings.ImportState
import com.karvo.ui.settings.ImportViewModel
import java.io.File
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var dir: File
    private lateinit var repo: FakeLessonRepository

    private fun lesson(student: String, day: Int) = Lesson(date = LocalDate.of(2026, 12, day), durationMinutes = 60, student = student)
    private val existing = listOf(lesson("Anna", 1).copy(id = 1), lesson("Bob", 2).copy(id = 2))

    @Before fun setUp() {
        dir = File(tmp.root, "backups")
        repo = FakeLessonRepository(existing)
    }

    private fun vm(text: String?, lessons: LessonRepository = repo, backupDir: File = dir) = ImportViewModel(
        lessons,
        BackupService(BackupStore(backupDir, TickingClock()), lessons, Dispatchers.Unconfined),
        reader = { text ?: throw IOException("gone") },
    )

    private fun backups() = BackupStore(dir, TickingClock()).list()

    @Test fun invalidFileShowsLineAndChangesNothing() = runTest {
        val vm = vm("date,student,duration_minutes,duration_hours,notes,paid_on\r\n2026-12-03,A,60,1,,\r\n2026-12-04,,60,1,,\r\n")
        vm.onFilePicked("content://x")
        assertEquals(ImportState.Invalid(3), vm.state.value)
        assertEquals(existing, repo.allLessons().sortedBy { it.id })
        assertTrue(backups().isEmpty())
    }

    @Test fun headerOnlyFileIsEmpty() = runTest {
        val vm = vm(lessonsToCsv(emptyList()))
        vm.onFilePicked("content://x")
        assertEquals(ImportState.Empty, vm.state.value)
        assertTrue(backups().isEmpty())
    }

    @Test fun allDuplicatesIsNothingNew() = runTest {
        val vm = vm(lessonsToCsv(existing))
        vm.onFilePicked("content://x")
        assertEquals(ImportState.NothingNew(2), vm.state.value)
        assertTrue(backups().isEmpty())
    }

    @Test fun previewThenConfirmBacksUpFirstThenAdds() = runTest {
        val vm = vm(lessonsToCsv(listOf(existing[0], lesson("Carla", 3), lesson("Dino", 4))))
        vm.onFilePicked("content://x")
        val preview = vm.state.value as ImportState.Preview
        assertEquals(2, preview.plan.toAdd.size)
        assertEquals(1, preview.plan.skipped)
        vm.confirm()
        assertEquals(ImportState.Done(2), vm.state.value)
        val backup = backups().single()
        assertEquals(BackupReason.BeforeImport, backup.reason)
        assertEquals(2, backup.lessonCount) // the lessons before the import
        assertEquals(setOf("Anna", "Bob", "Carla", "Dino"), repo.allLessons().map { it.student }.toSet())
    }

    @Test fun backupFailureStopsImport() = runTest {
        val blocked = File(tmp.root, "not-a-dir").apply { writeText("x") }
        val vm = vm(lessonsToCsv(listOf(lesson("Carla", 3))), backupDir = blocked)
        vm.onFilePicked("content://x")
        vm.confirm()
        assertEquals(ImportState.BackupFailed, vm.state.value)
        assertEquals(existing, repo.allLessons().sortedBy { it.id })
    }

    @Test fun addAllFailureIsFailed() = runTest {
        val failing = object : LessonRepository by repo {
            override suspend fun addAll(lessons: List<Lesson>) = throw IllegalStateException("db")
        }
        val vm = vm(lessonsToCsv(listOf(lesson("Carla", 3))), lessons = failing)
        vm.onFilePicked("content://x")
        vm.confirm()
        assertEquals(ImportState.Failed, vm.state.value)
        assertEquals(existing, repo.allLessons().sortedBy { it.id })
    }

    @Test fun readerFailureIsInvalidLineOne() = runTest {
        val vm = vm(null)
        vm.onFilePicked("content://x")
        assertEquals(ImportState.Invalid(1), vm.state.value)
    }

    @Test fun dismissReturnsToIdle() = runTest {
        val vm = vm(lessonsToCsv(listOf(lesson("Carla", 3))))
        vm.onFilePicked("content://x")
        vm.dismiss()
        assertEquals(ImportState.Idle, vm.state.value)
        assertEquals(2, repo.allLessons().size)
    }
}
