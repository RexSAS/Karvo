package com.karvo.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** An hourly pay rate. Exactly one rate is the default while any exist; a lesson with no (or an unknown) rate uses it. */
@Entity(tableName = "rates")
data class Rate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val cents: Long,
    /** ARGB. */
    val color: Int,
    val isDefault: Boolean = false,
)

const val MAX_RATE_TITLE = 30

/** True when a rate other than [id] already has [title], ignoring case and surrounding spaces. */
fun titleTaken(rates: List<Rate>, title: String, id: Long = 0): Boolean =
    rates.any { it.id != id && it.title.equals(title.trim(), ignoreCase = true) }

/** The swatches a rate can take; the first is given to the rate made from the old single hourly rate. */
val RATE_COLORS = listOf(0xFF4FA3E0, 0xFFE0B04F, 0xFF5CC08A, 0xFFE0675C, 0xFFA57BE0, 0xFFE07FB8, 0xFF5CC6C6, 0xFF9AA5B1).map { it.toInt() }

@Dao
interface RateDao {
    /** Default first, then in the order they were added. */
    @Query("SELECT * FROM rates ORDER BY isDefault DESC, id")
    fun observeAll(): Flow<List<Rate>>

    @Query("SELECT * FROM rates ORDER BY isDefault DESC, id")
    suspend fun getAll(): List<Rate>

    @Insert
    suspend fun insertRow(rate: Rate): Long

    @Update
    suspend fun updateRow(rate: Rate)

    @Query("DELETE FROM rates WHERE id = :id")
    suspend fun deleteRow(id: Long)

    @Query("UPDATE lessons SET rateId = :to WHERE rateId IS :from")
    suspend fun moveLessons(from: Long?, to: Long?)

    @Query("UPDATE rates SET isDefault = (id = :id)")
    suspend fun markDefault(id: Long)

    /** The first rate ever added becomes the default. */
    @Transaction
    suspend fun add(rate: Rate): Long = insertRow(rate.copy(id = 0, isDefault = getAll().isEmpty()))

    /** Lessons still on "the default" (no rate) keep the old default's rate, so their earnings don't change. */
    @Transaction
    suspend fun setDefault(id: Long) {
        val all = getAll()
        val old = all.firstOrNull { it.isDefault }
        if (all.none { it.id == id } || old?.id == id) return
        if (old != null) moveLessons(null, old.id)
        markDefault(id)
    }

    /** Its lessons move to the default. The default goes only when it is the last rate (its lessons then have none). */
    @Transaction
    suspend fun delete(id: Long): Boolean {
        val all = getAll()
        val rate = all.firstOrNull { it.id == id } ?: return false
        if (rate.isDefault && all.size > 1) return false
        moveLessons(id, all.firstOrNull { it.isDefault && it.id != id }?.id)
        deleteRow(id)
        return true
    }
}

class RateRepository(private val dao: RateDao) {
    fun observe(): Flow<List<Rate>> = dao.observeAll()
    suspend fun all(): List<Rate> = dao.getAll()

    /**
     * Adds ([Rate.id] 0) or updates a rate; the title is trimmed. Returns false for an invalid or vanished rate, or a
     * title another rate already has (ignoring case: CSV files name rates by title).
     */
    suspend fun save(rate: Rate): Boolean {
        val clean = rate.copy(title = rate.title.trim())
        if (clean.title.isEmpty() || clean.title.length > MAX_RATE_TITLE || clean.cents <= 0) return false
        val all = dao.getAll()
        if (titleTaken(all, clean.title, clean.id)) return false
        if (clean.id == 0L) { dao.add(clean); return true }
        val old = all.firstOrNull { it.id == clean.id } ?: return false
        dao.updateRow(clean.copy(isDefault = old.isDefault))
        return true
    }

    suspend fun setDefault(id: Long) = dao.setDefault(id)
    suspend fun delete(id: Long): Boolean = dao.delete(id)

    /** Before 3.8 there was one hourly rate in the settings: it becomes the first (default) rate, once. */
    suspend fun adoptLegacyRate(settings: SettingsRepository, title: String) {
        val cents = settings.hourlyRateCents.first() ?: return
        if (dao.getAll().isEmpty()) dao.add(Rate(title = title, cents = cents, color = RATE_COLORS[0]))
        settings.setHourlyRateCents(null)
    }
}
