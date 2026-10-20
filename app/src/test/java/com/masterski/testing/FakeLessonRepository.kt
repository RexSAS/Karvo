package com.masterski.testing

import com.masterski.data.Lesson
import com.masterski.data.LessonRepository
import com.masterski.data.normalizeForSave
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeLessonRepository(initial: List<Lesson> = emptyList()) : LessonRepository {
    private val lessons = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    private fun sorted(list: List<Lesson>) =
        list.sortedWith(compareByDescending<Lesson> { it.date }.thenByDescending { it.id })

    override fun observeLessons(): Flow<List<Lesson>> = lessons.map { sorted(it) }

    override fun observeStudentNames(): Flow<List<String>> =
        lessons.map { list -> sorted(list).map { it.student }.distinct() }

    override suspend fun get(id: Long): Lesson? = lessons.value.firstOrNull { it.id == id }

    override suspend fun save(lesson: Lesson): Long {
        val clean = normalizeForSave(lesson)
        if (clean.id == 0L) {
            val saved = clean.copy(id = nextId++)
            lessons.value = lessons.value + saved
            return saved.id
        }
        lessons.value = lessons.value.map { if (it.id == clean.id) clean else it }
        return clean.id
    }

    override suspend fun delete(lesson: Lesson) {
        lessons.value = lessons.value.filterNot { it.id == lesson.id }
    }

    override suspend fun restore(lesson: Lesson) {
        lessons.value = lessons.value.filterNot { it.id == lesson.id } + lesson
        if (lesson.id >= nextId) nextId = lesson.id + 1
    }

    override suspend fun allLessons(): List<Lesson> = sorted(lessons.value)
}
