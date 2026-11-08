package com.karvo.testing

import com.karvo.data.DayNoteRepository
import com.karvo.data.MAX_DAY_NOTE
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeDayNoteRepository(initial: Map<LocalDate, String> = emptyMap()) : DayNoteRepository {
    val notes = MutableStateFlow(initial)
    override fun observe() = notes
    override suspend fun all() = notes.value
    override suspend fun set(date: LocalDate, text: String?) {
        val t = text?.trim().orEmpty()
        require(t.length <= MAX_DAY_NOTE)
        notes.value = if (t.isEmpty()) notes.value - date else notes.value + (date to t)
    }
    override suspend fun replaceAll(notes: Map<LocalDate, String>) { this.notes.value = notes }
}
