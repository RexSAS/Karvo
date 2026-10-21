package com.masterski.ui.update

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.masterski.BuildConfig
import com.masterski.MasterSkiApp
import com.masterski.R
import com.masterski.data.backup.BackupReason
import com.masterski.data.backup.BackupService
import com.masterski.data.update.ApkDownloader
import com.masterski.data.update.IncompleteDownloadException
import com.masterski.data.update.InstallResult
import com.masterski.data.update.ReleaseInfo
import com.masterski.data.update.ReleaseSource
import com.masterski.data.update.UpdateInstaller
import com.masterski.domain.parseVersion
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object CheckFailed : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data object BackingUp : UpdateState
    data class Downloading(val read: Long, val total: Long) : UpdateState
    data object NeedsPermission : UpdateState
    data object Installing : UpdateState
    data class Failed(@StringRes val message: Int, val code: Int? = null) : UpdateState
}

/**
 * Update flow: check GitHub → (user taps Update) → back up lessons → download the APK → install permission
 * (once) → system installer. Activity-scoped, so the flow survives navigation and rotation.
 */
class UpdateViewModel(
    private val source: ReleaseSource,
    private val backups: BackupService,
    private val downloader: ApkDownloader,
    private val installer: UpdateInstaller,
    private val currentVersion: String,
    private val updatesDir: File,
    private val autoCheckEnabled: Boolean,
    private val claimAutoCheck: () -> Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var updateJob: Job? = null
    private var pendingApk: File? = null
    /** Set when we sent the user to the "Install unknown apps" page, so ON_RESUME knows to re-check. */
    private var awaitingPermission = false

    init {
        viewModelScope.launch {
            installer.results.collect { result ->
                _state.value = when (result) {
                    InstallResult.Aborted -> UpdateState.Idle
                    is InstallResult.Failed ->
                        if (result.signatureMismatch) {
                            UpdateState.Failed(R.string.update_signature_mismatch)
                        } else {
                            UpdateState.Failed(R.string.update_failed, result.code)
                        }
                }
            }
        }
    }

    private fun isNewer(release: ReleaseInfo): Boolean {
        val current = parseVersion(currentVersion) ?: return false
        return release.version > current
    }

    /** Cold-start check: release builds only, once per process, silent on failure. */
    fun checkOnStart() {
        if (!autoCheckEnabled || !claimAutoCheck()) return
        viewModelScope.launch {
            val release = source.latestRelease() ?: return@launch
            if (isNewer(release) && _state.value == UpdateState.Idle) _state.value = UpdateState.Available(release)
        }
    }

    /** Manual check from Settings: always reports an outcome. */
    fun checkNow() {
        _state.value = UpdateState.Checking
        viewModelScope.launch {
            val release = source.latestRelease()
            _state.value = when {
                release == null -> UpdateState.CheckFailed
                isNewer(release) -> UpdateState.Available(release)
                else -> UpdateState.UpToDate
            }
        }
    }

    fun update() {
        val release = (_state.value as? UpdateState.Available)?.release ?: return
        updateJob = viewModelScope.launch {
            _state.value = UpdateState.BackingUp
            try {
                backups.backup(BackupReason.BeforeUpdate(release.version.toString()))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = UpdateState.Failed(R.string.update_backup_failed)
                return@launch
            }

            clearUpdatesDir()
            val apk = File(updatesDir, "MasterSki-${release.version}.apk")
            _state.value = UpdateState.Downloading(0, release.apkSize)
            try {
                downloader.download(release.apkUrl, release.apkSize, apk) { read ->
                    _state.value = UpdateState.Downloading(read, release.apkSize)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: IncompleteDownloadException) {
                _state.value = UpdateState.Failed(R.string.update_download_incomplete)
                return@launch
            } catch (_: Exception) {
                _state.value = UpdateState.Failed(R.string.update_download_failed)
                return@launch
            }

            pendingApk = apk
            if (installer.canInstall()) install(apk) else _state.value = UpdateState.NeedsPermission
        }
    }

    private suspend fun install(apk: File) {
        _state.value = UpdateState.Installing
        try {
            installer.install(apk)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _state.value = UpdateState.Failed(R.string.update_failed, -1)
        }
    }

    fun cancelDownload() {
        val job = updateJob
        updateJob = null
        viewModelScope.launch {
            job?.cancelAndJoin()
            clearUpdatesDir()
            _state.value = UpdateState.Idle
        }
    }

    fun openPermissionSettings() {
        awaitingPermission = true
        installer.openInstallPermissionSettings()
    }

    /** Activity ON_RESUME: lets a held-back install confirmation appear, and re-checks the install permission. */
    fun onResume() {
        installer.setAppInForeground(true)
        onReturnedFromPermissionSettings()
    }

    fun onPause() = installer.setAppInForeground(false)

    /** Continues if the user granted "Install unknown apps", otherwise reports it. */
    fun onReturnedFromPermissionSettings() {
        if (!awaitingPermission || _state.value != UpdateState.NeedsPermission) return
        awaitingPermission = false
        val apk = pendingApk ?: return
        if (installer.canInstall()) {
            viewModelScope.launch { install(apk) }
        } else {
            _state.value = UpdateState.Failed(R.string.update_permission_denied)
        }
    }

    /** "Later", "OK" or "Cancel" on any dialog. */
    fun dismiss() {
        awaitingPermission = false
        _state.value = UpdateState.Idle
    }

    private fun clearUpdatesDir() {
        updatesDir.listFiles()?.forEach { it.delete() }
    }

    override fun onCleared() {
        updateJob?.cancel()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MasterSkiApp).container
                UpdateViewModel(
                    container.releaseSource, container.backupService, container.apkDownloader, container.updateInstaller,
                    currentVersion = BuildConfig.VERSION_NAME,
                    updatesDir = container.updatesDir,
                    autoCheckEnabled = !BuildConfig.DEBUG,
                    claimAutoCheck = container::claimUpdateCheck,
                )
            }
        }
    }
}
