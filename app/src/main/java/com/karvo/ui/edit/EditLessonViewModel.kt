package com.karvo.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karvo.KarvoApp
import com.karvo.data.DEFAULT_TIME_ZONE
import com.karvo.data.Lesson
import com.karvo.data.LessonRepository
import com.karvo.data.SettingsRepository
import com.karvo.domain.LessonError
import com.karvo.domain.validateLesson
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MAX_SUGGESTIONS = 5

data class EditLessonUiState(
    val isNew: Boolean = true,
    val date: LocalDate,
    val hoursText: String = "",
    val minutesText: String = "",
    val student: String = "",
    val notes: String = "",
    val errors: Set<LessonError>,
    val suggestions: List<String> = emptyList(),
    /** Fields the user has edited; inline errors are shown only for these. */
    val studentTouched: Boolean = false,
    val durationTouched: Boolean = false,
) {
    val isValid get() = errors.isEmpty()
}

class EditLessonViewModel(
    private val repo: LessonRepository,
    private val settings: SettingsRepository,
    private val lessonId: Long?,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EditLessonUiState(
            isNew = lessonId == null,
            date = LocalDate.now(clock.withZone(ZoneId.of(DEFAULT_TIME_ZONE))),
            errors = validateLesson("", 0, 0),
        ),
    )
    val uiState: StateFlow<EditLessonUiState> = _uiState.asStateFlow()

    private var original: Lesson? = null
    private var dateChosen = false
    private var studentNames: List<String> = emptyList()

    /** Set once a save or delete starts; further taps are ignored so onDone fires once. */
    private var busy = false

    init {
        viewModelScope.launch {
            repo.observeStudentNames().collect { names ->
                studentNames = names
                update { it }
            }
        }
        viewModelScope.launch {
            if (lessonId == null) {
                val zone = ZoneId.of(settings.timeZoneId.first())
                if (!dateChosen) update { it.copy(date = LocalDate.now(clock.withZone(zone))) }
            } else {
                repo.get(lessonId)?.let { lesson ->
                    original = lesson
                    dateChosen = true
                    update {
                        it.copy(
                            date = lesson.date,
                            hoursText = (lesson.durationMinutes / 60).toString(),
                            minutesText = (lesson.durationMinutes % 60).toString(),
                            student = lesson.student,
                            notes = lesson.notes.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    /** Applies [change], then recomputes errors and suggestions from the resulting fields. */
    private fun update(change: (EditLessonUiState) -> EditLessonUiState) {
        _uiState.value = change(_uiState.value).let { s ->
            s.copy(
                errors = validateLesson(s.student, s.hoursText.toIntOrNull() ?: 0, s.minutesText.toIntOrNull() ?: 0),
                suggestions = suggestionsFor(s.student),
            )
        }
    }

    private fun suggestionsFor(input: String): List<String> {
        val prefix = input.trim()
        if (prefix.isEmpty()) return emptyList()
        return studentNames
            .filter { it.startsWith(prefix, ignoreCase = true) && !it.equals(prefix, ignoreCase = true) }
            .take(MAX_SUGGESTIONS)
    }

    private fun isTwoDigits(text: String) = text.length <= 2 && text.all { it.isDigit() }

    fun onDateChange(date: LocalDate) {
        dateChosen = true
        update { it.copy(date = date) }
    }

    fun onHoursChange(text: String) {
        if (isTwoDigits(text)) update { it.copy(hoursText = text, durationTouched = true) }
    }

    fun onMinutesChange(text: String) {
        if (isTwoDigits(text)) update { it.copy(minutesText = text, durationTouched = true) }
    }

    fun onQuickPick(minutes: Int) {
        update { it.copy(hoursText = (minutes / 60).toString(), minutesText = (minutes % 60).toString(), durationTouched = true) }
    }

    fun onStudentChange(text: String) = update { it.copy(student = text, studentTouched = true) }

    fun onNotesChange(text: String) = update { it.copy(notes = text) }

    fun save(onDone: () -> Unit) {
        val s = _uiState.value
        if (!s.isValid || busy) return
        busy = true
        val minutes = (s.hoursText.toIntOrNull() ?: 0) * 60 + (s.minutesText.toIntOrNull() ?: 0)
        viewModelScope.launch {
            repo.save(
                Lesson(
                    id = original?.id ?: 0L,
                    date = s.date,
                    durationMinutes = minutes,
                    student = s.student,
                    notes = s.notes,
                ),
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val lesson = original ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            repo.delete(lesson)
            onDone()
        }
    }

    companion object {
        fun factory(lessonId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KarvoApp).container
                EditLessonViewModel(container.lessonRepository, container.settingsRepository, lessonId)
            }
        }
    }
}
