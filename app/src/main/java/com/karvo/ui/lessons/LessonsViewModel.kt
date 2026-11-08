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
    private val notesRepo: com.karvo.data.DayNoteRepository,
    private val settings: com.karvo.data.SettingsRepository,
) : ViewModel() {

    /** Today in the app's time zone (it decides which notes are upcoming). */
    val today: StateFlow<java.time.LocalDate> = settings.timeZoneId
        .map { java.time.LocalDate.now(runCatching { java.time.ZoneId.of(it) }.getOrDefault(java.time.ZoneId.systemDefault())) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, java.time.LocalDate.now())

    /** Today's and upcoming day notes, soonest first, at most 5. */
    val notes: StateFlow<List<Pair<java.time.LocalDate, String>>> = kotlinx.coroutines.flow.combine(notesRepo.observe(), today) { all, t ->
        all.filterKeys { !it.isBefore(t) }.toSortedMap().entries.take(5).map { it.key to it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveNote(date: java.time.LocalDate, text: String?) { viewModelScope.launch { notesRepo.set(date, text) } }


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
                LessonsViewModel(container.lessonRepository, container.dayNoteRepository, container.settingsRepository)
            }
        }
    }
}
