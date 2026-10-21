package com.masterski.data.backup

import com.masterski.data.Lesson
import com.masterski.data.LessonRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Backs up the current lessons and gives access to the store, off the main thread. */
class BackupService(
    private val store: BackupStore,
    private val lessons: LessonRepository,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun backup(reason: BackupReason): Backup {
        val all = lessons.allLessons()
        return withContext(io) { store.create(reason, all) }
    }

    suspend fun list(): List<Backup> = withContext(io) { store.list() }

    suspend fun read(fileName: String): List<Lesson> = withContext(io) { store.read(fileName) }

    suspend fun oldestToBeRemoved(): Backup? = withContext(io) { store.oldestToBeRemoved() }
}
