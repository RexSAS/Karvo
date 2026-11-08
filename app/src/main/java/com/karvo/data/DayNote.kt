package com.karvo.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** One short note per day (same rules on the PC and the iPhone: trimmed, at most [MAX_DAY_NOTE] characters). */
@Entity(tableName = "day_notes")
data class DayNote(@PrimaryKey val date: LocalDate, val text: String)

const val MAX_DAY_NOTE = 200

@Dao
interface DayNoteDao {
    @Query("SELECT * FROM day_notes ORDER BY date") fun observeAll(): Flow<List<DayNote>>
    @Query("SELECT * FROM day_notes ORDER BY date") suspend fun all(): List<DayNote>
    @Upsert suspend fun upsert(note: DayNote)
    @Query("DELETE FROM day_notes WHERE date = :date") suspend fun delete(date: LocalDate)
    @Query("DELETE FROM day_notes") suspend fun deleteAll()
    @Upsert suspend fun upsertAll(notes: List<DayNote>)

    @Transaction suspend fun replaceAll(notes: List<DayNote>) { deleteAll(); upsertAll(notes) }
}

interface DayNoteRepository {
    fun observe(): Flow<Map<LocalDate, String>>
    suspend fun all(): Map<LocalDate, String>
    /** Blank or null text deletes the day's note. */
    suspend fun set(date: LocalDate, text: String?)
    suspend fun replaceAll(notes: Map<LocalDate, String>)
}

class RoomDayNoteRepository(private val dao: DayNoteDao) : DayNoteRepository {
    override fun observe() = dao.observeAll().map { list -> list.associate { it.date to it.text } }
    override suspend fun all() = dao.all().associate { it.date to it.text }
    override suspend fun set(date: LocalDate, text: String?) {
        val t = text?.trim().orEmpty()
        require(t.length <= MAX_DAY_NOTE) { "Note too long" }
        if (t.isEmpty()) dao.delete(date) else dao.upsert(DayNote(date, t))
    }
    override suspend fun replaceAll(notes: Map<LocalDate, String>) = dao.replaceAll(notes.map { DayNote(it.key, it.value) })
}
