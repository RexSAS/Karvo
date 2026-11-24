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
    private val events: com.karvo.data.EventRepository? = null,
    private val rates: com.karvo.data.RateRepository? = null,
) {
    private suspend fun pay() = com.karvo.domain.Pay(rates?.all().orEmpty())

    suspend fun backup(reason: BackupReason): Backup {
        val all = lessons.allLessons()
        val evs = events?.all().orEmpty()
        val pay = pay()
        return withContext(io) { store.create(reason, all, evs, pay::titleOf) }
    }

    suspend fun readEvents(fileName: String): List<com.karvo.data.Event>? = withContext(io) { store.readEvents(fileName) }

    suspend fun list(): List<Backup> = withContext(io) { store.list() }

    suspend fun read(fileName: String): List<Lesson> {
        val pay = pay()
        return withContext(io) { store.read(fileName, pay::idOfTitle) }
    }

    suspend fun oldestToBeRemoved(): Backup? = withContext(io) { store.oldestToBeRemoved() }
}
