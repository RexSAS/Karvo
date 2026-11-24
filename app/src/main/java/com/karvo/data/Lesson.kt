package com.karvo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val durationMinutes: Int,
    val student: String,
    val notes: String? = null,
    /** Day the school paid this lesson; null = unpaid. */
    val paidOn: LocalDate? = null,
    /** The [Rate] this lesson is paid at; null (or a deleted rate) = the default rate. */
    val rateId: Long? = null,
)
