package com.masterski.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.masterski.MasterSkiApp
import com.masterski.data.DEFAULT_TIME_ZONE
import com.masterski.data.LessonRepository
import com.masterski.data.SettingsRepository
import com.masterski.data.backup.Backup
import com.masterski.data.backup.BackupReason
import com.masterski.data.backup.BackupService
import com.masterski.domain.BackupFormatException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RestoreResult {
    data class Done(val count: Int) : RestoreResult
    /** The backup file is damaged; [line] is where (0 = the file couldn't be opened). */
    data class ReadError(val line: Int) : RestoreResult
    data object BackupError : RestoreResult
}

data class RestoreUiState(
    /** 1 = "Restore this backup?", 2 = "Last check". */
    val step: Int = 1,
    val understood: Boolean = false,
    val acceptedRisk: Boolean = false,
    val currentCount: Int = 0,
    val backup: Backup? = null,
    /** True while the "Back up your current lessons first?" dialog is shown. */
    val askBackupFirst: Boolean = false,
    /** The backup a safety backup would remove (null if fewer than 3 exist). */
    val oldestToRemove: Backup? = null,
    val zoneId: String = DEFAULT_TIME_ZONE,
    val busy: Boolean = false,
    val result: RestoreResult? = null,
)

/** Guards a restore behind two confirmations, then replaces every lesson with the backup's. */
class RestoreViewModel(
    private val fileName: String,
    private val service: BackupService,
    private val lessons: LessonRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RestoreUiState())
    val uiState: StateFlow<RestoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val backup = service.list().firstOrNull { it.fileName == fileName }
            val count = lessons.allLessons().size
            val oldest = service.oldestToBeRemoved()
            val zone = settings.timeZoneId.first()
            _uiState.update { it.copy(backup = backup, currentCount = count, oldestToRemove = oldest, zoneId = zone) }
        }
    }

    fun setUnderstood(value: Boolean) = _uiState.update { it.copy(understood = value) }

    fun next() = _uiState.update { if (it.understood) it.copy(step = 2) else it }

    fun back() = _uiState.update { it.copy(step = 1, acceptedRisk = false) }

    fun setAcceptedRisk(value: Boolean) = _uiState.update { it.copy(acceptedRisk = value) }

    fun requestRestore() = _uiState.update { if (it.step == 2 && it.acceptedRisk) it.copy(askBackupFirst = true) else it }

    fun dismissAsk() = _uiState.update { it.copy(askBackupFirst = false) }

    /**
     * Reads the backup first, so a safety backup that rotates the source file away can't break the restore;
     * then optionally backs up the current lessons, then replaces them all.
     */
    fun restore(backupFirst: Boolean) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(askBackupFirst = false, busy = true) }
        viewModelScope.launch {
            val result = runRestore(backupFirst)
            _uiState.update { it.copy(busy = false, result = result) }
        }
    }

    private suspend fun runRestore(backupFirst: Boolean): RestoreResult {
        val source = try {
            service.read(fileName)
        } catch (e: BackupFormatException) {
            return RestoreResult.ReadError(e.line)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return RestoreResult.ReadError(0)
        }
        if (backupFirst) {
            try {
                service.backup(BackupReason.BeforeRestore)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return RestoreResult.BackupError
            }
        }
        lessons.replaceAll(source)
        return RestoreResult.Done(source.size)
    }

    companion object {
        fun factory(fileName: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MasterSkiApp).container
                RestoreViewModel(fileName, container.backupService, container.lessonRepository, container.settingsRepository)
            }
        }
    }
}
