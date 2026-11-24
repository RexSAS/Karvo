package com.karvo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karvo.KarvoApp
import com.karvo.data.Authenticator
import com.karvo.data.DEFAULT_SEASON_START_MONTH
import com.karvo.data.DEFAULT_TIME_ZONE
import com.karvo.data.LanguageChoice
import com.karvo.data.LanguageController
import com.karvo.data.LessonRepository
import com.karvo.data.SettingsRepository
import com.karvo.data.ThemeChoice
import com.karvo.data.Rate
import com.karvo.data.RateRepository
import com.karvo.domain.Pay
import com.karvo.domain.csvFileName
import com.karvo.domain.lessonsToCsv
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    /** Default first. */
    val rates: List<Rate> = emptyList(),
    val timeZoneId: String = DEFAULT_TIME_ZONE,
    val theme: ThemeChoice = ThemeChoice.ALPINE_NIGHT,
    val language: LanguageChoice = LanguageChoice.SYSTEM,
    val seasonStartMonth: Int = DEFAULT_SEASON_START_MONTH,
    val biometricReveal: Boolean = true,
    /** False when the phone has no screen lock; the switch is then disabled. */
    val biometricAvailable: Boolean = false,
)

/** Region zone ids (containing '/'), excluding Etc/ and SystemV/, sorted; computed once. */
val availableTimeZones: List<String> = ZoneId.getAvailableZoneIds()
    .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
    .sorted()

/** CSV text plus the number of lesson rows it contains. */
data class CsvExport(val csv: String, val lessonCount: Int)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val lessons: LessonRepository,
    private val language: LanguageController,
    private val rates: RateRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val authenticator: Authenticator = Authenticator.None,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(language = language.current()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            rates.observe().collect { list -> _uiState.update { it.copy(rates = list) } }
        }
        viewModelScope.launch {
            settings.timeZoneId.collect { id -> _uiState.update { it.copy(timeZoneId = id) } }
        }
        viewModelScope.launch {
            settings.theme.collect { choice -> _uiState.update { it.copy(theme = choice) } }
        }
        viewModelScope.launch {
            settings.seasonStartMonth.collect { month -> _uiState.update { it.copy(seasonStartMonth = month) } }
        }
        viewModelScope.launch {
            settings.biometricReveal.collect { on -> _uiState.update { it.copy(biometricReveal = on) } }
        }
        refreshBiometricAvailability()
    }

    /** Adds ([Rate.id] 0) or updates a rate; invalid ones are ignored (the dialog doesn't offer them). */
    fun saveRate(rate: Rate) {
        viewModelScope.launch { rates.save(rate) }
    }

    fun setDefaultRate(rate: Rate) {
        viewModelScope.launch { rates.setDefault(rate.id) }
    }

    fun deleteRate(rate: Rate) {
        viewModelScope.launch { rates.delete(rate.id) }
    }

    fun onTimeZoneSelected(id: String) {
        if (id !in availableTimeZones) return
        viewModelScope.launch { settings.setTimeZoneId(id) }
    }

    /** Turning protection off needs a successful prompt first, so nobody else can just switch it off. */
    fun onBiometricRevealChange(on: Boolean, promptTitle: String) {
        viewModelScope.launch {
            if (on || !authenticator.isAvailable() || authenticator.authenticate(promptTitle)) settings.setBiometricReveal(on)
        }
    }

    /** Re-read on resume: the user may have just set or removed the phone's screen lock. */
    fun refreshBiometricAvailability() {
        _uiState.update { it.copy(biometricAvailable = authenticator.isAvailable()) }
    }

    fun onSeasonStartSelected(month: Int) {
        if (month !in 1..12) return
        viewModelScope.launch { settings.setSeasonStartMonth(month) }
    }

    fun onThemeSelected(choice: ThemeChoice) {
        viewModelScope.launch { settings.setTheme(choice) }
    }

    fun onLanguageSelected(choice: LanguageChoice) {
        language.set(choice)
        _uiState.update { it.copy(language = choice) }
    }

    suspend fun exportData(): CsvExport {
        val all = lessons.allLessons()
        return CsvExport(lessonsToCsv(all, Pay(rates.all())::titleOf), all.size)
    }

    suspend fun buildCsv(): String = exportData().csv

    suspend fun exportFileName(): String {
        val zone = ZoneId.of(settings.timeZoneId.first())
        return csvFileName(LocalDate.now(clock.withZone(zone)))
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KarvoApp).container
                SettingsViewModel(
                    container.settingsRepository, container.lessonRepository, container.languageController, container.rateRepository,
                    authenticator = container.authenticator,
                )
            }
        }
    }
}
