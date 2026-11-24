package com.karvo.testing

import com.karvo.data.LessonRepository
import com.karvo.data.Rate
import com.karvo.data.RateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [RateDao]; moving lessons between rates goes through [lessons] when given. */
class FakeRateDao(private val lessons: LessonRepository? = null) : RateDao {
    private val rates = MutableStateFlow<List<Rate>>(emptyList())
    private var nextId = 1L
    private fun sorted(list: List<Rate>) = list.sortedWith(compareByDescending<Rate> { it.isDefault }.thenBy { it.id })

    override fun observeAll(): Flow<List<Rate>> = rates.map(::sorted)
    override suspend fun getAll(): List<Rate> = sorted(rates.value)
    override suspend fun insertRow(rate: Rate): Long = rate.copy(id = nextId++).also { rates.value += it }.id
    override suspend fun updateRow(rate: Rate) { rates.value = rates.value.map { if (it.id == rate.id) rate else it } }
    override suspend fun deleteRow(id: Long) { rates.value = rates.value.filter { it.id != id } }
    override suspend fun markDefault(id: Long) { rates.value = rates.value.map { it.copy(isDefault = it.id == id) } }
    override suspend fun moveLessons(from: Long?, to: Long?) {
        lessons?.allLessons()?.filter { it.rateId == from }?.forEach { lessons.save(it.copy(rateId = to)) }
    }
}
