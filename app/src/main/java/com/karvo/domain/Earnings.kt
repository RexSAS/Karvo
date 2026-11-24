package com.karvo.domain

import com.karvo.data.Lesson
import com.karvo.data.Rate

/** Half-up rounded earnings in cents, computed from summed minutes. */
fun earningsCents(totalMinutes: Long, hourlyRateCents: Long): Long =
    (totalMinutes * hourlyRateCents + 30) / 60

/** The user's hourly rates. A lesson with no rate, or a rate that's gone, is paid at the default one. */
class Pay(val rates: List<Rate>) {
    val default: Rate? = rates.firstOrNull { it.isDefault } ?: rates.firstOrNull()
    private val byId = rates.associateBy { it.id }

    /** False with no rates: earnings are then hidden. */
    val isSet: Boolean get() = default != null

    fun rateOf(lesson: Lesson): Rate? = byId[lesson.rateId] ?: default

    /** Half-up rounded once over the whole sum, so one rate gives exactly [earningsCents] of the summed minutes. */
    fun earnings(lessons: Iterable<Lesson>): Long? {
        if (default == null) return null
        return (lessons.sumOf { it.durationMinutes.toLong() * rateOf(it)!!.cents } + 30) / 60
    }

    /** How a lesson's rate is written in CSV files: by title, since rate ids differ between phones. */
    fun titleOf(lesson: Lesson): String = rateOf(lesson)?.title.orEmpty()

    /** The rate a CSV title names (ignoring case); null (= the default) when there's no such rate. */
    fun idOfTitle(title: String): Long? = rates.firstOrNull { it.title.equals(title.trim(), ignoreCase = true) }?.id

    companion object {
        val NONE = Pay(emptyList())
    }
}
