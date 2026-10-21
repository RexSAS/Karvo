package com.karvo.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karvo.KarvoApp
import com.karvo.data.Lesson
import com.karvo.data.Authenticator
import com.karvo.data.LessonRepository
import com.karvo.data.SettingsRepository
import com.karvo.data.mayRevealEarnings
import com.karvo.domain.PaidStatus
import com.karvo.domain.earningsCents
import com.karvo.domain.paidStatus
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MonthDetailUiState(
    val month: YearMonth,
    val lessons: List<Lesson> = emptyList(),
    val totalMinutes: Long = 0,
    val earningsCents: Long? = null,
    val paidStatus: PaidStatus = PaidStatus.UNPAID,
    /** Latest paid date among the month's lessons. */
    val paidOn: LocalDate? = null,
    /** Today in the configured time zone: the default date when ticking the month as paid. */
    val today: LocalDate = LocalDate.now(),
    val revealed: Boolean = false,
)

class MonthDetailViewModel(
    private val lessons: LessonRepository,
    private val settings: SettingsRepository,
    private val month: YearMonth,
    private val clock: Clock = Clock.systemUTC(),
    private val authenticator: Authenticator = Authenticator.None,
) : ViewModel() {

    /** In-memory only: hidden each time the screen opens and whenever it stops. */
    private val revealed = MutableStateFlow(false)

    val uiState: StateFlow<MonthDetailUiState> = combine(
        lessons.observeLessons(), settings.hourlyRateCents, settings.timeZoneId, revealed,
    ) { all, rate, zone, isRevealed ->
        val inMonth = all.filter { YearMonth.from(it.date) == month }
        val total = inMonth.sumOf { it.durationMinutes.toLong() }
        MonthDetailUiState(
            month = month,
            lessons = inMonth,
            totalMinutes = total,
            earningsCents = rate?.let { earningsCents(total, it) },
            paidStatus = paidStatus(inMonth),
            paidOn = inMonth.mapNotNull { it.paidOn }.maxOrNull(),
            today = LocalDate.now(clock.withZone(ZoneId.of(zone))),
            revealed = isRevealed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthDetailUiState(month))

    /** Gives the month's unpaid lessons the paid date [date]; lessons already paid keep theirs. */
    fun markPaid(date: LocalDate) {
        viewModelScope.launch { lessons.markMonthPaid(month, date) }
    }

    /** Same rule as Overview: hiding is immediate, showing may ask for fingerprint/PIN. */
    fun toggleReveal(promptTitle: String) {
        if (revealed.value) {
            revealed.value = false
            return
        }
        viewModelScope.launch {
            if (mayRevealEarnings(settings, authenticator, promptTitle)) revealed.value = true
        }
    }

    fun hide() {
        revealed.value = false
    }

    fun markUnpaid() {
        viewModelScope.launch { lessons.markMonthUnpaid(month) }
    }

    companion object {
        fun factory(month: YearMonth): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KarvoApp).container
                MonthDetailViewModel(container.lessonRepository, container.settingsRepository, month, authenticator = container.authenticator)
            }
        }
    }
}
