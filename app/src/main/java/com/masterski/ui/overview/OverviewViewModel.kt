package com.masterski.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.masterski.MasterSkiApp
import com.masterski.data.LessonRepository
import com.masterski.data.SettingsRepository
import com.masterski.domain.MonthSummary
import com.masterski.domain.earningsCents
import com.masterski.domain.summarizeByMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class MonthRow(val summary: MonthSummary, val earningsCents: Long?)

data class OverviewUiState(
    val totalMinutes: Long = 0,
    val totalEarningsCents: Long? = null,
    val months: List<MonthRow> = emptyList(),
    val rateSet: Boolean = false,
    val revealed: Boolean = false,
)

class OverviewViewModel(lessons: LessonRepository, settings: SettingsRepository) : ViewModel() {

    /** In-memory only; never persisted, so earnings are hidden on every app start. */
    private val revealed = MutableStateFlow(false)

    val uiState: StateFlow<OverviewUiState> = combine(
        lessons.observeLessons(), settings.hourlyRateCents, revealed,
    ) { all, rate, isRevealed ->
        val summaries = summarizeByMonth(all)
        val total = summaries.sumOf { it.totalMinutes }
        OverviewUiState(
            totalMinutes = total,
            totalEarningsCents = rate?.let { earningsCents(total, it) },
            months = summaries.map { MonthRow(it, rate?.let { r -> earningsCents(it.totalMinutes, r) }) },
            rateSet = rate != null,
            revealed = isRevealed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverviewUiState())

    fun toggleReveal() {
        revealed.update { !it }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MasterSkiApp).container
                OverviewViewModel(container.lessonRepository, container.settingsRepository)
            }
        }
    }
}
