package com.karvo.testing

import com.karvo.data.Lesson
import com.karvo.data.LessonRepository
import kotlinx.coroutines.CompletableDeferred

/** Delegating repository whose save/delete suspend until [gate] is completed. */
class GatedLessonRepository(private val delegate: LessonRepository) : LessonRepository by delegate {
    val gate = CompletableDeferred<Unit>()

    override suspend fun save(lesson: Lesson): Long {
        gate.await()
        return delegate.save(lesson)
    }

    override suspend fun delete(lesson: Lesson) {
        gate.await()
        delegate.delete(lesson)
    }
}
