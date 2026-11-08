package com.karvo.testing

import com.karvo.data.Event
import com.karvo.data.EventRepository
import com.karvo.data.MAX_EVENT
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeEventRepository(initial: List<Event> = emptyList()) : EventRepository {
    val events = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1
    private fun clean(t: String) = t.trim().also { require(it.isNotEmpty() && it.length <= MAX_EVENT) }
    override fun observe() = events
    override suspend fun all() = events.value
    override suspend fun add(date: LocalDate, text: String): Long { val id = nextId++; events.value = events.value + Event(id, date, clean(text)); return id }
    override suspend fun update(event: Event): Boolean {
        if (events.value.none { it.id == event.id }) return false
        events.value = events.value.map { if (it.id == event.id) event.copy(text = clean(event.text)) else it }; return true
    }
    override suspend fun delete(id: Long): Boolean { val before = events.value.size; events.value = events.value.filter { it.id != id }; return events.value.size < before }
    override suspend fun replaceAll(events: List<Event>) { this.events.value = events.mapIndexed { i, e -> e.copy(id = i + 1L) }; nextId = events.size + 1L }
}
