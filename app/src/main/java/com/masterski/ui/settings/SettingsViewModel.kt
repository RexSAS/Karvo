package com.masterski.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.masterski.MasterSkiApp
import com.masterski.data.DEFAULT_TIME_ZONE
import com.masterski.data.LessonRepository
import com.masterski.data.SettingsRepository
import com.masterski.domain.RateParseResult
import com.masterski.domain.csvFileName
import com.masterski.domain.formatRateText
import com.masterski.domain.lessonsToCsv
import com.masterski.domain.normalizeRateInput
import com.masterski.domain.parseRateCents
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
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = settings.hourlyRateCents.first()
            if (stored != null) {
                // Do not clobber text the user has already started typing.
                _uiState.update { if (it.rateText.isEmpty()) it.copy(rateText = formatRateText(stored)) else it }
            }
        }
        viewModelScope.launch {
            settings.timeZoneId.collect { id -> _uiState.update { it.copy(timeZoneId = id) } }
        }
    }

    fun onRateChange(input: String) {
        _uiState.update { it.copy(rateText = normalizeRateInput(input), rateError = false) }
    }

    fun saveRate() {
        viewModelScope.launch {
            when (val result = parseRateCents(_uiState.value.rateText)) {
                RateParseResult.Cleared -> {
                    settings.setHourlyRateCents(null)
                    _uiState.update { it.copy(rateText = "", rateError = false) }
                }
                is RateParseResult.Valid -> {
                    settings.setHourlyRateCents(result.cents)
                    _uiState.update { it.copy(rateText = formatRateText(result.cents), rateError = false) }
                }
                RateParseResult.Invalid -> _uiState.update { it.copy(rateError = true) }
            }
        }
    }

    fun onTimeZoneSelected(id: String) {
        if (id !in availableTimeZones) return
        viewModelScope.launch { settings.setTimeZoneId(id) }
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
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MasterSkiApp).container
                SettingsViewModel(container.settingsRepository, container.lessonRepository)
            }
        }
    }
}
