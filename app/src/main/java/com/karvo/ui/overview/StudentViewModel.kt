package com.karvo.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karvo.KarvoApp
import com.karvo.data.Authenticator
import com.karvo.data.LessonRepository
import com.karvo.data.SettingsRepository
import com.karvo.data.mayRevealEarnings
import com.karvo.domain.StudentDetail
import com.karvo.domain.computeStudent
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudentUiState(
    val loading: Boolean = true,
    /** Null once loaded means the student has no lessons any more (all deleted). */
    val detail: StudentDetail? = null,
    val rateSet: Boolean = false,
    val revealed: Boolean = false,
)

/** One student's page: [key] is their name as matched by studentKey. */
class StudentViewModel(
    private val lessons: LessonRepository,
    private val settings: SettingsRepository,
    private val key: String,
    private val clock: Clock = Clock.systemUTC(),
    private val authenticator: Authenticator = Authenticator.None,
) : ViewModel() {

    /** In-memory only: hidden each time the screen opens and whenever it stops. */
    private val revealed = MutableStateFlow(false)

    val uiState: StateFlow<StudentUiState> = combine(
        lessons.observeLessons(), settings.hourlyRateCents, settings.timeZoneId, settings.seasonStartMonth, revealed,
    ) { all, rate, zone, seasonStart, isRevealed ->
        val today = LocalDate.now(clock.withZone(ZoneId.of(zone)))
        StudentUiState(loading = false, detail = computeStudent(all, key, today, seasonStart, rate), rateSet = rate != null, revealed = isRevealed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudentUiState())

    /** Same rule as Overview: hiding is immediate, showing may ask for fingerprint/PIN. */
    fun toggleReveal(promptTitle: String) {
        if (revealed.value) {
            revealed.value = false
            return
        }
        viewModelScope.launch { if (mayRevealEarnings(settings, authenticator, promptTitle)) revealed.value = true }
    }

    fun hide() {
        revealed.value = false
    }

    companion object {
        fun factory(key: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KarvoApp).container
                StudentViewModel(container.lessonRepository, container.settingsRepository, key, authenticator = container.authenticator)
            }
        }
    }
}
