package com.karvo.ui.overview

import com.karvo.data.Lesson
import com.karvo.domain.Season
import java.time.YearMonth
import com.karvo.domain.seasonOf
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
import kotlinx.coroutines.launch

data class MonthRow(val summary: MonthSummary, val earningsCents: Long?)

/** What Overview counts: the season we're in (the default every time), a past season, or every lesson ever. */
sealed interface OverviewScope {
    data object ThisSeason : OverviewScope
    data class Past(val season: Season) : OverviewScope
    data object Lifetime : OverviewScope
}

data class OverviewUiState(
    val scope: OverviewScope = OverviewScope.ThisSeason,
    /** The season shown ([OverviewScope.ThisSeason] or [OverviewScope.Past]); null for Lifetime. */
    val scopeSeason: Season? = null,
    /** Past seasons with lessons, newest first: the picker's other choices. */
    val pastSeasons: List<Season> = emptyList(),
    /** Totals, months and stats below are for [scope]. */
    val totalMinutes: Long = 0,
    val totalEarningsCents: Long? = null,
    val months: List<MonthRow> = emptyList(),
    val rateSet: Boolean = false,
    val revealed: Boolean = false,
    /** True until the first data arrives; the screen shows nothing meanwhile. */
    val loading: Boolean = true,
    /** For [scope]; null when it has no lessons. */
    val stats: OverviewStats? = null,
    /** Every season (whatever the scope); null when there are no lessons at all. */
    val seasons: SeasonStats? = null,
    val unpaid: Unpaid = Unpaid(0, 0, null),
)

class OverviewViewModel(
    lessons: LessonRepository,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val authenticator: Authenticator = Authenticator.None,
) : ViewModel() {

    /** In-memory only; never persisted, so earnings are hidden on every app start. */
    private val revealed = MutableStateFlow(false)
    /** In-memory only: Overview opens on this season every time. */
    private val scope = MutableStateFlow<OverviewScope>(OverviewScope.ThisSeason)

    private data class Inputs(val all: List<Lesson>, val rate: Long?, val revealed: Boolean, val zone: String, val seasonStart: Int)

    val uiState: StateFlow<OverviewUiState> = combine(
        combine(lessons.observeLessons(), settings.hourlyRateCents, revealed, settings.timeZoneId, settings.seasonStartMonth, ::Inputs),
        scope,
    ) { (all, rate, isRevealed, zone, seasonStart), chosen ->
        val today = LocalDate.now(clock.withZone(ZoneId.of(zone)))
        val current = seasonOf(today, seasonStart)
        val pastSeasons = all.map { seasonOf(it.date, seasonStart) }.distinct().filter { it.startYear < current.startYear }.sortedByDescending { it.startYear }
        // A past season that no longer has lessons (or a changed season start) falls back to this season.
        val shown = if (chosen is OverviewScope.Past && chosen.season !in pastSeasons) OverviewScope.ThisSeason else chosen
        val season = when (shown) { OverviewScope.ThisSeason -> current; is OverviewScope.Past -> shown.season; OverviewScope.Lifetime -> null }
        val inScope = if (season == null) all else all.filter { it.date in season.range() }
        // A past season's chart ends at its last month with lessons, not at today.
        val lastMonth = if (shown is OverviewScope.Past) inScope.maxOf { YearMonth.from(it.date) } else YearMonth.from(today)
        val summaries = summarizeByMonth(inScope)
        val total = summaries.sumOf { it.totalMinutes }
        OverviewUiState(
            scope = shown,
            scopeSeason = season,
            pastSeasons = pastSeasons,
            totalMinutes = total,
            totalEarningsCents = rate?.let { earningsCents(total, it) },
            months = summaries.map { MonthRow(it, rate?.let { r -> earningsCents(it.totalMinutes, r) }) },
            rateSet = rate != null,
            revealed = isRevealed,
            loading = false,
            stats = if (inScope.isEmpty()) null else computeStats(inScope, today, rate, lastMonth),
            seasons = if (all.isEmpty()) null else computeSeasons(all, today, seasonStart, rate),
            unpaid = unpaidTotals(all, today, rate),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverviewUiState())

    fun hide() {
        revealed.value = false
    }

    /** What Overview counts; see [OverviewScope]. */
    fun show(chosen: OverviewScope) {
        scope.value = chosen
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
