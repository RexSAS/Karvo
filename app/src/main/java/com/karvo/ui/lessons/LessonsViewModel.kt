package com.karvo.ui.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karvo.KarvoApp
import com.karvo.data.Lesson
import com.karvo.data.LessonRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LessonGroup(val month: YearMonth, val lessons: List<Lesson>) {
    val totalMinutes: Long get() = lessons.sumOf { it.durationMinutes.toLong() }
}

class LessonsViewModel(
    private val repo: LessonRepository,
    private val eventsRepo: com.karvo.data.EventRepository,
    private val settings: com.karvo.data.SettingsRepository,
) : ViewModel() {

    /** Today in the app's time zone (it decides which notes are upcoming). */
    val today: StateFlow<java.time.LocalDate> = settings.timeZoneId
        .map { java.time.LocalDate.now(runCatching { java.time.ZoneId.of(it) }.getOrDefault(java.time.ZoneId.systemDefault())) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, java.time.LocalDate.now())

    /** Events of today and the next 3 days, for the home card. */
    val upcoming: StateFlow<List<Triple<java.time.LocalDate, Int, List<com.karvo.data.Event>>>> =
        kotlinx.coroutines.flow.combine(eventsRepo.observe(), today) { all, t -> com.karvo.data.upcomingDays(all, t) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    /** Lessons grouped by month, newest month first (repository order is date DESC); null while loading. */
    val groups: StateFlow<List<LessonGroup>?> = repo.observeLessons()
        .map<List<Lesson>, List<LessonGroup>?> { lessons ->
            lessons.groupBy { YearMonth.from(it.date) }
                .map { (month, items) -> LessonGroup(month, items) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(lesson: Lesson) {
        viewModelScope.launch { repo.delete(lesson) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KarvoApp).container
                LessonsViewModel(container.lessonRepository, container.eventRepository, container.settingsRepository)
            }
        }
    }
}
