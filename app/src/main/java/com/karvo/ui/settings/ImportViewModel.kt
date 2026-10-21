package com.karvo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karvo.KarvoApp
import com.karvo.data.LessonRepository
import com.karvo.data.TextFileReader
import com.karvo.data.backup.BackupReason
import com.karvo.data.backup.BackupService
import com.karvo.domain.BackupFormatException
import com.karvo.domain.ImportPlan
import com.karvo.domain.parseLessonsCsv
import com.karvo.domain.planImport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImportState {
    data object Idle : ImportState
    data object Reading : ImportState
    /** The file can't be read; [line] is where the first bad record starts (1 when the file itself failed). */
    data class Invalid(val line: Int) : ImportState
    data object Empty : ImportState
    data class NothingNew(val total: Int) : ImportState
    data class Preview(val plan: ImportPlan) : ImportState
    data object Importing : ImportState
    data class Done(val count: Int) : ImportState
    data object BackupFailed : ImportState
    data object Failed : ImportState
}

/**
 * Import from a CSV file: read and check it all, preview what's new, back up, then add in one go.
 * Nothing changes until [confirm], and nothing changes if any step fails.
 */
class ImportViewModel(
    private val lessons: LessonRepository,
    private val backups: BackupService,
    private val reader: TextFileReader,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun onFilePicked(uri: String) {
        _state.value = ImportState.Reading
        viewModelScope.launch {
            _state.value = try {
                val incoming = parseLessonsCsv(reader.read(uri))
                val plan = planImport(lessons.allLessons(), incoming)
                when {
                    incoming.isEmpty() -> ImportState.Empty
                    plan.toAdd.isEmpty() -> ImportState.NothingNew(incoming.size)
                    else -> ImportState.Preview(plan)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupFormatException) {
                ImportState.Invalid(e.line)
            } catch (_: Exception) {
                ImportState.Invalid(1)
            }
        }
    }

    fun confirm() {
        val plan = (_state.value as? ImportState.Preview)?.plan ?: return
        _state.value = ImportState.Importing
        viewModelScope.launch {
            try {
                backups.backup(BackupReason.BeforeImport)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = ImportState.BackupFailed
                return@launch
            }
            _state.value = try {
                lessons.addAll(plan.toAdd)
                ImportState.Done(plan.toAdd.size)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ImportState.Failed
            }
        }
    }

    fun dismiss() {
        if (_state.value != ImportState.Importing) _state.value = ImportState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KarvoApp).container
                ImportViewModel(container.lessonRepository, container.backupService, container.textFileReader)
            }
        }
    }
}
