package com.masterski.ui

import com.masterski.R
import com.masterski.data.Lesson
import com.masterski.data.backup.BackupReason
import com.masterski.data.backup.BackupService
import com.masterski.data.backup.BackupStore
import com.masterski.data.backup.TickingClock
import com.masterski.data.update.ApkDownloader
import com.masterski.data.update.IncompleteDownloadException
import com.masterski.data.update.InstallResult
import com.masterski.data.update.ReleaseInfo
import com.masterski.data.update.ReleaseSource
import com.masterski.data.update.UpdateInstaller
import com.masterski.domain.parseVersion
import com.masterski.testing.FakeLessonRepository
import com.masterski.testing.MainDispatcherRule
import com.masterski.ui.update.UpdateState
import com.masterski.ui.update.UpdateViewModel
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private fun release(v: String) = ReleaseInfo(parseVersion(v)!!, "MasterSki $v", "notes", "https://example.com/MasterSki.apk", 10)

private class FakeReleaseSource(var release: ReleaseInfo?) : ReleaseSource {
    var calls = 0
    override suspend fun latestRelease(): ReleaseInfo? = release.also { calls++ }
}

private class FakeDownloader : ApkDownloader {
    enum class Mode { OK, INCOMPLETE, HANG }
    var mode = Mode.OK
    var calls = 0
    override suspend fun download(url: String, expectedSize: Long, dest: File, onProgress: (Long) -> Unit) {
        calls++
        dest.parentFile?.mkdirs()
        when (mode) {
            Mode.OK -> { dest.writeBytes(ByteArray(expectedSize.toInt())); onProgress(expectedSize) }
            Mode.INCOMPLETE -> throw IncompleteDownloadException(expectedSize, 3)
            Mode.HANG -> {
                dest.writeBytes(ByteArray(3)); onProgress(3)
                try { awaitCancellation() } finally { dest.delete() }
            }
        }
    }
}

private class FakeInstaller : UpdateInstaller {
    var allowed = true
    val installed = mutableListOf<File>()
    var settingsOpened = 0
    val foreground = mutableListOf<Boolean>()
    override val results = MutableSharedFlow<InstallResult>(extraBufferCapacity = 4)
    override fun canInstall() = allowed
    override fun openInstallPermissionSettings() { settingsOpened++ }
    override suspend fun install(apk: File) { installed += apk }
    override fun setAppInForeground(inForeground: Boolean) { foreground += inForeground }
    var reopened = 0
    override fun reopenConfirmation(): Boolean { reopened++; return true }
}

class UpdateViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var updatesDir: File
    private lateinit var backupDir: File
    private val source = FakeReleaseSource(release("1.3"))
    private val downloader = FakeDownloader()
    private val installer = FakeInstaller()
    private val repo = FakeLessonRepository(listOf(Lesson(id = 1, date = LocalDate.of(2026, 1, 1), durationMinutes = 60, student = "A")))

    @Before fun setUp() {
        updatesDir = File(tmp.root, "updates")
        backupDir = File(tmp.root, "backups")
    }

    private fun vm(autoCheck: Boolean = true, claim: () -> Boolean = { true }) = UpdateViewModel(
        source, BackupService(BackupStore(backupDir, TickingClock()), repo, Dispatchers.Unconfined),
        downloader, installer, currentVersion = "1.2", updatesDir = updatesDir, autoCheckEnabled = autoCheck, claimAutoCheck = claim,
    )

    @Test fun autoCheckOffersNewer() = runTest {
        val vm = vm(); vm.checkOnStart()
        assertEquals(UpdateState.Available(release("1.3")), vm.state.value)
    }

    @Test fun autoCheckSameVersionStaysIdle() = runTest {
        source.release = release("1.2.0")
        val vm = vm(); vm.checkOnStart()
        assertEquals(UpdateState.Idle, vm.state.value)
    }

    @Test fun autoCheckFailureIsSilent() = runTest {
        source.release = null
        val vm = vm(); vm.checkOnStart()
        assertEquals(UpdateState.Idle, vm.state.value)
    }

    @Test fun autoCheckDisabledInDebug() = runTest {
        vm(autoCheck = false).checkOnStart()
        assertEquals(0, source.calls)
    }

    @Test fun autoCheckRunsOncePerProcess() = runTest {
        vm(claim = { false }).checkOnStart()
        assertEquals(0, source.calls)
    }

    @Test fun manualCheckUpToDate() = runTest {
        source.release = release("1.2")
        val vm = vm(); vm.checkNow()
        assertEquals(UpdateState.UpToDate, vm.state.value)
    }

    @Test fun manualCheckFailure() = runTest {
        source.release = null
        val vm = vm(); vm.checkNow()
        assertEquals(UpdateState.CheckFailed, vm.state.value)
    }

    @Test fun updateBacksUpThenDownloadsThenInstalls() = runTest {
        val vm = vm(); vm.checkOnStart(); vm.update()
        assertEquals(UpdateState.Installing, vm.state.value)
        assertEquals(listOf(File(updatesDir, "MasterSki-1.3.apk")), installer.installed)
        val backups = BackupStore(backupDir, TickingClock()).list()
        assertEquals(listOf<BackupReason>(BackupReason.BeforeUpdate("1.3")), backups.map { it.reason })
    }

    @Test fun backupFailureStopsBeforeDownload() = runTest {
        backupDir.parentFile!!.mkdirs(); backupDir.writeText("not a directory")
        val vm = vm(); vm.checkOnStart(); vm.update()
        assertEquals(UpdateState.Failed(R.string.update_backup_failed), vm.state.value)
        assertEquals(0, downloader.calls)
    }

    @Test fun noPermissionAsksThenInstallsWhenGranted() = runTest {
        installer.allowed = false
        val vm = vm(); vm.checkOnStart(); vm.update()
        assertEquals(UpdateState.NeedsPermission, vm.state.value)
        vm.openPermissionSettings()
        assertEquals(1, installer.settingsOpened)
        installer.allowed = true
        vm.onReturnedFromPermissionSettings()
        assertEquals(UpdateState.Installing, vm.state.value)
        assertEquals(1, installer.installed.size)
    }

    @Test fun permissionDeniedFails() = runTest {
        installer.allowed = false
        val vm = vm(); vm.checkOnStart(); vm.update(); vm.openPermissionSettings()
        vm.onReturnedFromPermissionSettings()
        assertEquals(UpdateState.Failed(R.string.update_permission_denied), vm.state.value)
    }

    @Test fun resumeWithoutOpeningSettingsDoesNothing() = runTest {
        installer.allowed = false
        val vm = vm(); vm.checkOnStart(); vm.update()
        vm.onReturnedFromPermissionSettings()
        assertEquals(UpdateState.NeedsPermission, vm.state.value)
    }

    @Test fun incompleteDownloadFails() = runTest {
        downloader.mode = FakeDownloader.Mode.INCOMPLETE
        val vm = vm(); vm.checkOnStart(); vm.update()
        assertEquals(UpdateState.Failed(R.string.update_download_incomplete), vm.state.value)
        assertTrue(installer.installed.isEmpty())
    }

    @Test fun cancelDownloadReturnsIdle() = runTest {
        downloader.mode = FakeDownloader.Mode.HANG
        val vm = vm(); vm.checkOnStart(); vm.update()
        assertEquals(UpdateState.Downloading(3, 10), vm.state.value)
        vm.cancelDownload()
        assertEquals(UpdateState.Idle, vm.state.value)
        assertTrue(updatesDir.listFiles().orEmpty().isEmpty())
    }

    @Test fun installerAbortedReturnsIdle() = runTest {
        val vm = vm(); vm.checkOnStart(); vm.update()
        installer.results.emit(InstallResult.Aborted)
        assertEquals(UpdateState.Idle, vm.state.value)
    }

    @Test fun installerSignatureMismatchFails() = runTest {
        val vm = vm(); vm.checkOnStart(); vm.update()
        installer.results.emit(InstallResult.Failed(5, signatureMismatch = true))
        assertEquals(UpdateState.Failed(R.string.update_signature_mismatch), vm.state.value)
    }

    @Test fun laterDismissesTheOffer() = runTest {
        val vm = vm(); vm.checkOnStart(); vm.dismiss()
        assertEquals(UpdateState.Idle, vm.state.value)
    }

    @Test fun resumeAndPauseTellTheInstaller() = runTest {
        val vm = vm()
        vm.onResume(); vm.onPause()
        assertEquals(listOf(true, false), installer.foreground)
    }

    @Test fun installingCanBeClosed() = runTest {
        val vm = vm(); vm.checkOnStart(); vm.update()
        vm.dismiss()
        assertEquals(UpdateState.Idle, vm.state.value)
    }

    @Test fun openInstallerReopensTheSystemConfirmation() = runTest {
        val vm = vm(); vm.checkOnStart(); vm.update()
        vm.reopenInstaller()
        assertEquals(1, installer.reopened)
        assertEquals(UpdateState.Installing, vm.state.value)
    }
}
