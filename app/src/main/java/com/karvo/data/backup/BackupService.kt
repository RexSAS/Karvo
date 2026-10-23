package com.karvo.data.backup

import com.karvo.data.Lesson
import com.karvo.data.LessonRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Backs up the current lessons and gives access to the store, off the main thread. */
class BackupService(
    private val store: BackupStore,
    private val lessons: LessonRepository,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    /** How many backups to keep; free users get fewer. */
    private val keep: suspend () -> Int = { MAX_BACKUPS },
) {
    suspend fun backup(reason: BackupReason): Backup {
        val all = lessons.allLessons()
        val limit = keep()
        return withContext(io) { store.create(reason, all, limit) }
    }

    suspend fun list(): List<Backup> = withContext(io) { store.list() }

    suspend fun read(fileName: String): List<Lesson> = withContext(io) { store.read(fileName) }

    suspend fun oldestToBeRemoved(): Backup? {
        val limit = keep()
        return withContext(io) { store.oldestToBeRemoved(limit) }
    }
}
