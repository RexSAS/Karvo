package com.karvo.data

import com.karvo.domain.MAX_DURATION_MINUTES
import kotlinx.coroutines.flow.Flow

interface LessonRepository {
    fun observeLessons(): Flow<List<Lesson>>
    fun observeStudentNames(): Flow<List<String>>
    suspend fun get(id: Long): Lesson?
    suspend fun save(lesson: Lesson): Long
    suspend fun delete(lesson: Lesson)
    suspend fun restore(lesson: Lesson)
    suspend fun allLessons(): List<Lesson>

    /** Replaces every lesson with [lessons] (validated first; ids are reassigned). Nothing changes on failure. */
    suspend fun replaceAll(lessons: List<Lesson>)
}

/** Trims student/notes (blank notes become null) and validates; shared by all repository implementations. */
fun normalizeForSave(lesson: Lesson): Lesson {
    val student = lesson.student.trim()
    require(student.isNotEmpty()) { "Student must not be blank" }
    require(lesson.durationMinutes in 1..MAX_DURATION_MINUTES) {
        "Duration must be in 1..$MAX_DURATION_MINUTES minutes"
    }
    return lesson.copy(student = student, notes = lesson.notes?.trim()?.ifEmpty { null })
}

class RoomLessonRepository(private val dao: LessonDao) : LessonRepository {
    override fun observeLessons(): Flow<List<Lesson>> = dao.observeAll()
    override fun observeStudentNames(): Flow<List<String>> = dao.observeStudentNames()
    override suspend fun get(id: Long): Lesson? = dao.getById(id)

    override suspend fun save(lesson: Lesson): Long {
        val clean = normalizeForSave(lesson)
        return if (clean.id == 0L) {
            dao.insert(clean)
        } else {
            dao.update(clean)
            clean.id
        }
    }

    override suspend fun delete(lesson: Lesson) = dao.delete(lesson)
    override suspend fun restore(lesson: Lesson) {
        dao.insert(lesson)
    }
    override suspend fun allLessons(): List<Lesson> = dao.getAll()

    override suspend fun replaceAll(lessons: List<Lesson>) {
        dao.replaceAll(lessons.map { normalizeForSave(it.copy(id = 0)) })
    }
}
