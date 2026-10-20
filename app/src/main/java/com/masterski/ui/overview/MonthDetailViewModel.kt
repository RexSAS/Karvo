package com.masterski.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.masterski.MasterSkiApp
import com.masterski.data.Lesson
import com.masterski.data.LessonRepository
import com.masterski.data.SettingsRepository
import com.masterski.domain.earningsCents
import java.time.YearMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MonthDetailUiState(
    val month: YearMonth,
    val lessons: List<Lesson> = emptyList(),
    val totalMinutes: Long = 0,
    val earningsCents: Long? = null,
)

class MonthDetailViewModel(
    lessons: LessonRepository,
    settings: SettingsRepository,
    private val month: YearMonth,
) : ViewModel() {

    val uiState: StateFlow<MonthDetailUiState> = combine(lessons.observeLessons(), settings.hourlyRateCents) { all, rate ->
        val inMonth = all.filter { YearMonth.from(it.date) == month }
        val total = inMonth.sumOf { it.durationMinutes.toLong() }
        MonthDetailUiState(month, inMonth, total, rate?.let { earningsCents(total, it) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthDetailUiState(month))

    companion object {
        fun factory(month: YearMonth): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MasterSkiApp).container
                MonthDetailViewModel(container.lessonRepository, container.settingsRepository, month)
            }
        }
    }
}
