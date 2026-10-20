package com.masterski.ui.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.masterski.MasterSkiApp
import com.masterski.data.Lesson
import com.masterski.data.LessonRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LessonGroup(val month: YearMonth, val lessons: List<Lesson>)

class LessonsViewModel(private val repo: LessonRepository) : ViewModel() {

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
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MasterSkiApp).container
                LessonsViewModel(container.lessonRepository)
            }
        }
    }
}
