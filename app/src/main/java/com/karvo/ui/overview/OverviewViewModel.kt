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
import com.karvo.domain.MonthSummary
import com.karvo.domain.OverviewStats
import com.karvo.domain.SeasonStats
import com.karvo.domain.Unpaid
import com.karvo.domain.computeSeasons
import com.karvo.domain.unpaidTotals
import com.karvo.domain.computeStats
import com.karvo.domain.earningsCents
import com.karvo.domain.summarizeByMonth
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MonthRow(val summary: MonthSummary, val earningsCents: Long?)

data class OverviewUiState(
    val totalMinutes: Long = 0,
    val totalEarningsCents: Long? = null,
    val months: List<MonthRow> = emptyList(),
    val rateSet: Boolean = false,
    val revealed: Boolean = false,
    /** True until the first data arrives; the screen shows nothing meanwhile. */
    val loading: Boolean = true,
    /** Null when there are no lessons. */
    val stats: OverviewStats? = null,
    /** Null when there are no lessons. */
    val seasons: SeasonStats? = null,
    val unpaid: Unpaid = Unpaid(0, 0, null),
    /** Without Pro only total hours show; money and statistics are locked. */
    val pro: Boolean = false,
)

class OverviewViewModel(
    lessons: LessonRepository,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val authenticator: Authenticator = Authenticator.None,
) : ViewModel() {

    /** In-memory only; never persisted, so earnings are hidden on every app start. */
    private val revealed = MutableStateFlow(false)

    val uiState: StateFlow<OverviewUiState> = combine(
        lessons.observeLessons(), settings.hourlyRateCents, revealed, settings.timeZoneId,
        settings.seasonStartMonth.combine(settings.pro, ::Pair),
    ) { all, rate, isRevealed, zone, (seasonStart, pro) ->
        val today = LocalDate.now(clock.withZone(ZoneId.of(zone)))
        val summaries = summarizeByMonth(all)
        val total = summaries.sumOf { it.totalMinutes }
        OverviewUiState(
            totalMinutes = total,
            totalEarningsCents = rate?.let { earningsCents(total, it) },
            months = summaries.map { MonthRow(it, rate?.let { r -> earningsCents(it.totalMinutes, r) }) },
            rateSet = rate != null,
            revealed = isRevealed,
            loading = false,
            stats = if (all.isEmpty()) null else computeStats(all, today, rate),
            seasons = if (all.isEmpty()) null else computeSeasons(all, today, seasonStart, rate),
            unpaid = unpaidTotals(all, today, rate),
            pro = pro,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverviewUiState())

    fun hide() {
        revealed.value = false
    }

    /** Hiding is immediate; showing may first ask for fingerprint/PIN ([promptTitle] is the prompt's title). */
    fun toggleReveal(promptTitle: String) {
        if (revealed.value) {
            revealed.value = false
            return
        }
        viewModelScope.launch {
            if (mayRevealEarnings(settings, authenticator, promptTitle)) revealed.value = true
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KarvoApp).container
                OverviewViewModel(container.lessonRepository, container.settingsRepository, authenticator = container.authenticator)
            }
        }
    }
}
