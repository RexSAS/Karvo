package com.karvo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karvo.KarvoApp
import com.karvo.data.DEFAULT_TIME_ZONE
import com.karvo.data.LanguageChoice
import com.karvo.data.LanguageController
import com.karvo.data.LessonRepository
import com.karvo.data.SettingsRepository
import com.karvo.data.ThemeChoice
import com.karvo.domain.RateParseResult
import com.karvo.domain.csvFileName
import com.karvo.domain.formatRateText
import com.karvo.domain.lessonsToCsv
import com.karvo.domain.normalizeRateInput
import com.karvo.domain.parseRateCents
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
    val rateText: String = "",
    val rateError: Boolean = false,
    val timeZoneId: String = DEFAULT_TIME_ZONE,
    val theme: ThemeChoice = ThemeChoice.ALPINE_NIGHT,
    val language: LanguageChoice = LanguageChoice.SYSTEM,
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
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(language = language.current()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** True when the rate text was edited since it was loaded or last saved. */
    private var rateDirty = false

    /** Last known stored rate (valid once [storedKnown]), used to skip redundant writes. */
    private var storedRate: Long? = null
    private var storedKnown = false

    init {
        viewModelScope.launch {
            val stored = settings.hourlyRateCents.first()
            if (!storedKnown) {
                storedRate = stored
                storedKnown = true
            }
            if (stored != null) {
                // Do not clobber text the user has already started typing.
                _uiState.update { if (it.rateText.isEmpty()) it.copy(rateText = formatRateText(stored)) else it }
            }
        }
        viewModelScope.launch {
            settings.timeZoneId.collect { id -> _uiState.update { it.copy(timeZoneId = id) } }
        }
        viewModelScope.launch {
            settings.theme.collect { choice -> _uiState.update { it.copy(theme = choice) } }
        }
    }

    fun onRateChange(input: String) {
        rateDirty = true
        _uiState.update { it.copy(rateText = normalizeRateInput(input), rateError = false) }
    }

    /**
     * Saves the typed rate. Idempotent: called on Done, focus loss, ON_STOP and when the screen leaves;
     * it writes only if the text was edited and differs from the stored rate. Invalid text leaves storage unchanged.
     */
    fun saveRate() {
        if (!rateDirty) return
        val cents = when (val result = parseRateCents(_uiState.value.rateText)) {
            RateParseResult.Cleared -> null
            is RateParseResult.Valid -> result.cents
            RateParseResult.Invalid -> {
                _uiState.update { it.copy(rateError = true) }
                return
            }
        }
        rateDirty = false
        _uiState.update { it.copy(rateText = cents?.let(::formatRateText).orEmpty(), rateError = false) }
        if (storedKnown && cents == storedRate) return
        storedRate = cents
        storedKnown = true
        viewModelScope.launch { settings.setHourlyRateCents(cents) }
    }

    fun onTimeZoneSelected(id: String) {
        if (id !in availableTimeZones) return
        viewModelScope.launch { settings.setTimeZoneId(id) }
    }

    fun onThemeSelected(choice: ThemeChoice) {
        viewModelScope.launch { settings.setTheme(choice) }
    }

    /** Saves a typed rate first: applying a language recreates the activity. */
    fun onLanguageSelected(choice: LanguageChoice) {
        saveRate()
        language.set(choice)
        _uiState.update { it.copy(language = choice) }
    }

    suspend fun exportData(): CsvExport {
        val all = lessons.allLessons()
        return CsvExport(lessonsToCsv(all), all.size)
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
                SettingsViewModel(container.settingsRepository, container.lessonRepository, container.languageController)
            }
        }
    }
}
