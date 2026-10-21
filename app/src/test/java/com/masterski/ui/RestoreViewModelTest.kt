package com.masterski.ui

import com.masterski.data.Lesson
import com.masterski.data.backup.BackupReason
import com.masterski.data.backup.BackupService
import com.masterski.data.backup.BackupStore
import com.masterski.data.backup.TickingClock
import com.masterski.testing.FakeLessonRepository
import com.masterski.testing.FakeSettingsRepository
import com.masterski.testing.MainDispatcherRule
import com.masterski.ui.backup.BackupEvent
import com.masterski.ui.backup.BackupsViewModel
import com.masterski.ui.backup.RestoreResult
import com.masterski.ui.backup.RestoreViewModel
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RestoreViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var dir: File
    private lateinit var store: BackupStore
    private lateinit var repo: FakeLessonRepository
    private lateinit var service: BackupService

    private fun lessons(prefix: String, n: Int) =
        (1..n).map { Lesson(date = LocalDate.of(2026, 1, it), durationMinutes = 60, student = "$prefix$it") }

    private suspend fun students() = repo.allLessons().map { it.student }.toSet()

    @Before fun setUp() {
        dir = File(tmp.root, "backups")
        store = BackupStore(dir, TickingClock())
        repo = FakeLessonRepository(lessons("Now", 2).mapIndexed { i, l -> l.copy(id = i + 1L) })
        service = BackupService(store, repo, Dispatchers.Unconfined)
    }

    private fun vm(fileName: String) = RestoreViewModel(fileName, service, repo, FakeSettingsRepository())

    @Test fun continueNeedsUnderstood() = runTest {
        val vm = vm(store.create(BackupReason.Manual, lessons("Old", 3)).fileName)
        vm.next()
        assertEquals(1, vm.uiState.value.step)
        vm.setUnderstood(true); vm.next()
        assertEquals(2, vm.uiState.value.step)
    }

    @Test fun restoreNeedsAcceptedRisk() = runTest {
        val vm = vm(store.create(BackupReason.Manual, lessons("Old", 3)).fileName)
        vm.setUnderstood(true); vm.next()
        vm.requestRestore()
        assertFalse(vm.uiState.value.askBackupFirst)
        vm.setAcceptedRisk(true); vm.requestRestore()
        assertTrue(vm.uiState.value.askBackupFirst)
    }

    @Test fun showsCountsBeforeRestoring() = runTest {
        val vm = vm(store.create(BackupReason.Manual, lessons("Old", 3)).fileName)
        val s = vm.uiState.first { it.backup != null }
        assertEquals(2, s.currentCount)
        assertEquals(3, s.backup!!.lessonCount)
    }

    @Test fun restoreWithoutBackupReplacesLessons() = runTest {
        val vm = vm(store.create(BackupReason.Manual, lessons("Old", 3)).fileName)
        vm.restore(backupFirst = false)
        assertEquals(RestoreResult.Done(3), vm.uiState.value.result)
        assertEquals(setOf("Old1", "Old2", "Old3"), students())
        assertEquals(1, store.list().size)
    }

    @Test fun restoreSurvivesRotatingAwayTheSource() = runTest {
        val source = store.create(BackupReason.Manual, lessons("Old", 3))
        store.create(BackupReason.Manual, lessons("Mid", 1))
        store.create(BackupReason.Manual, lessons("New", 1))
        val vm = vm(source.fileName)
        vm.uiState.first { it.oldestToRemove != null }
        assertEquals(source.fileName, vm.uiState.value.oldestToRemove!!.fileName)
        vm.restore(backupFirst = true)
        assertEquals(RestoreResult.Done(3), vm.uiState.value.result)
        assertEquals(setOf("Old1", "Old2", "Old3"), students())
        assertFalse(File(dir, source.fileName).exists())
        val newest = store.list().first()
        assertEquals(BackupReason.BeforeRestore, newest.reason)
        assertEquals(setOf("Now1", "Now2"), store.read(newest.fileName).map { it.student }.toSet())
    }

    @Test fun corruptBackupChangesNothing() = runTest {
        dir.mkdirs(); File(dir, "backup-1-manual.csv").writeText("date,student,duration_minutes,duration_hours,notes\r\nbad,A,60,1,\r\n")
        val vm = vm("backup-1-manual.csv")
        vm.restore(backupFirst = true)
        assertEquals(RestoreResult.ReadError(2), vm.uiState.value.result)
        assertEquals(setOf("Now1", "Now2"), students())
        assertEquals(1, store.list().size)
    }

    @Test fun backupNowAddsManualBackup() = runTest {
        val vm = BackupsViewModel(service, FakeSettingsRepository())
        val events = mutableListOf<BackupEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }
        vm.backupNow()
        assertEquals(listOf(BackupEvent.Saved(2)), events)
        assertEquals(1, vm.uiState.first { it.backups.isNotEmpty() }.backups.size)
        assertEquals(BackupReason.Manual, vm.uiState.value.backups.single().reason)
        job.cancel()
    }
}
