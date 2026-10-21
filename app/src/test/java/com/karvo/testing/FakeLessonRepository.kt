package com.karvo.testing

import com.karvo.data.Lesson
import com.karvo.data.LessonRepository
import com.karvo.data.normalizeForSave
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

class FakeLessonRepository(initial: List<Lesson> = emptyList()) : LessonRepository {
    private val lessons = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    private fun sorted(list: List<Lesson>) =
        list.sortedWith(compareByDescending<Lesson> { it.date }.thenByDescending { it.id })

    override fun observeLessons(): Flow<List<Lesson>> = lessons.map { sorted(it) }

    override fun observeStudentNames(): Flow<List<String>> =
        lessons.map { list ->
            list.groupBy { it.student }.entries
                .sortedWith(
                    compareByDescending<Map.Entry<String, List<Lesson>>> { e -> e.value.maxOf { it.date } }
                        .thenByDescending { e -> e.value.maxOf { it.id } },
                )
                .map { it.key }
        }

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

    override suspend fun replaceAll(lessons: List<Lesson>) {
        val clean = lessons.map { normalizeForSave(it.copy(id = 0)) }
        this.lessons.value = clean.map { it.copy(id = nextId++) }
    }

    override suspend fun addAll(lessons: List<Lesson>) {
        val clean = lessons.map { normalizeForSave(it.copy(id = 0)) }
        this.lessons.value = this.lessons.value + clean.map { it.copy(id = nextId++) }
    }

    override suspend fun markMonthPaid(month: YearMonth, paidOn: LocalDate) {
        lessons.value = lessons.value.map {
            if (YearMonth.from(it.date) == month && it.paidOn == null) it.copy(paidOn = paidOn) else it
        }
    }

    override suspend fun markMonthUnpaid(month: YearMonth) {
        lessons.value = lessons.value.map { if (YearMonth.from(it.date) == month) it.copy(paidOn = null) else it }
    }
}
