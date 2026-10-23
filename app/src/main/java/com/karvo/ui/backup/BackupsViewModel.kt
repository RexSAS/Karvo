package com.karvo.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karvo.KarvoApp
import com.karvo.data.DEFAULT_TIME_ZONE
import com.karvo.data.SettingsRepository
import com.karvo.data.backup.Backup
import com.karvo.data.backup.BackupReason
import com.karvo.data.backup.BackupService
import com.karvo.data.backup.FREE_MAX_BACKUPS
import com.karvo.data.backup.MAX_BACKUPS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupsUiState(
    val backups: List<Backup> = emptyList(),
    val zoneId: String = DEFAULT_TIME_ZONE,
    val busy: Boolean = false,
    /** False until the first listing has loaded. */
    val loaded: Boolean = false,
    val maxBackups: Int = FREE_MAX_BACKUPS,
)

sealed interface BackupEvent {
    data class Saved(val count: Int) : BackupEvent
    data object Failed : BackupEvent
}

class BackupsViewModel(private val service: BackupService, settings: SettingsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(BackupsUiState())
    val uiState: StateFlow<BackupsUiState> = _uiState.asStateFlow()

    private val _events = Channel<BackupEvent>(Channel.BUFFERED)
    val events: Flow<BackupEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { settings.timeZoneId.collect { id -> _uiState.update { it.copy(zoneId = id) } } }
        viewModelScope.launch {
            settings.pro.collect { pro -> _uiState.update { it.copy(maxBackups = if (pro) MAX_BACKUPS else FREE_MAX_BACKUPS) } }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val list = service.list()
            _uiState.update { it.copy(backups = list, loaded = true) }
        }
    }

    fun backupNow() {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            val event = try {
                BackupEvent.Saved(service.backup(BackupReason.Manual).lessonCount)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                BackupEvent.Failed
            }
            _uiState.update { it.copy(backups = service.list(), busy = false, loaded = true) }
            _events.send(event)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KarvoApp).container
                BackupsViewModel(container.backupService, container.settingsRepository)
            }
        }
    }
}
