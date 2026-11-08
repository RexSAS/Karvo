package com.karvo.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** An event on a day (several per day): same rules on the PC and the iPhone, trimmed, 1..[MAX_EVENT] characters. */
@Entity(tableName = "events", indices = [Index("date")])
data class Event(@PrimaryKey(autoGenerate = true) val id: Long = 0, val date: LocalDate, val text: String)

const val MAX_EVENT = 200

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date, id") fun observeAll(): Flow<List<Event>>
    @Query("SELECT * FROM events ORDER BY date, id") suspend fun all(): List<Event>
    @Insert suspend fun insert(event: Event): Long
    @Insert suspend fun insertAll(events: List<Event>)
    @Update suspend fun update(event: Event): Int
    @Query("DELETE FROM events WHERE id = :id") suspend fun delete(id: Long): Int
    @Query("DELETE FROM events") suspend fun deleteAll()

    @Transaction suspend fun replaceAll(events: List<Event>) { deleteAll(); insertAll(events.map { it.copy(id = 0) }) }
}

interface EventRepository {
    fun observe(): Flow<List<Event>>
    suspend fun all(): List<Event>
    suspend fun add(date: LocalDate, text: String): Long
    /** False when the event no longer exists. */
    suspend fun update(event: Event): Boolean
    suspend fun delete(id: Long): Boolean
    suspend fun replaceAll(events: List<Event>)
}

private fun clean(text: String): String = text.trim().also { require(it.isNotEmpty() && it.length <= MAX_EVENT) { "Event must be 1..$MAX_EVENT characters" } }

class RoomEventRepository(private val dao: EventDao) : EventRepository {
    override fun observe() = dao.observeAll()
    override suspend fun all() = dao.all()
    override suspend fun add(date: LocalDate, text: String) = dao.insert(Event(date = date, text = clean(text)))
    override suspend fun update(event: Event) = dao.update(event.copy(text = clean(event.text))) > 0
    override suspend fun delete(id: Long) = dao.delete(id) > 0
    override suspend fun replaceAll(events: List<Event>) = dao.replaceAll(events.map { it.copy(text = clean(it.text)) })
}

/** Today..today+[days] that have events, soonest first: (date, days from today, events). */
fun upcomingDays(events: List<Event>, today: LocalDate, days: Int = 3): List<Triple<LocalDate, Int, List<Event>>> =
    (0..days).map { today.plusDays(it.toLong()) to it }
        .map { (d, n) -> Triple(d, n, events.filter { it.date == d }.sortedBy { it.id }) }
        .filter { it.third.isNotEmpty() }
