package com.karvo.domain

import android.net.Uri
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Longest video a moment can be; recording stops by itself here. */
const val MAX_VIDEO_MILLIS = 10_000L

/** A photo or short video in the gallery's Karvo album. */
data class Moment(val uri: Uri, val isVideo: Boolean, val takenAt: Instant)

data class MomentDay(val date: LocalDate, val moments: List<Moment>)

/** Newest day first; within a day, newest moment first. */
fun groupMomentsByDay(moments: List<Moment>, zone: ZoneId): List<MomentDay> =
    moments.sortedByDescending { it.takenAt }
        .groupBy { it.takenAt.atZone(zone).toLocalDate() }
        .map { (date, items) -> MomentDay(date, items) }

/** How many moments were taken in each month, for the lesson list headers. */
fun momentCountsByMonth(moments: List<Moment>, zone: ZoneId): Map<YearMonth, Int> =
    moments.groupingBy { YearMonth.from(it.takenAt.atZone(zone)) }.eachCount()

/** A day keeps its best moments only: one for free users, three with Pro. */
fun momentsPerDay(pro: Boolean): Int = if (pro) 3 else 1

/** The moments taken on [date], oldest first. */
fun momentsOn(date: LocalDate, moments: List<Moment>, zone: ZoneId): List<Moment> =
    moments.filter { it.takenAt.atZone(zone).toLocalDate() == date }.sortedBy { it.takenAt }

/** The month as calendar cells, Monday first: leading nulls pad the first week, trailing nulls fill the last. */
fun calendarCells(month: YearMonth): List<LocalDate?> {
    val lead = month.atDay(1).dayOfWeek.value - 1
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List(lead) { null } + days
    return cells + List((7 - cells.size % 7) % 7) { null }
}
