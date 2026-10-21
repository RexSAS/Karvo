package com.karvo.domain

enum class LessonError { StudentBlank, DurationZero, DurationTooLong, MinutesOver59 }

const val MAX_DURATION_MINUTES = 1440

fun validateLesson(student: String, hours: Int, minutes: Int): Set<LessonError> {
    val total = hours * 60 + minutes
    return buildSet {
        if (minutes > 59) add(LessonError.MinutesOver59)
        if (total == 0) add(LessonError.DurationZero)
        if (total > MAX_DURATION_MINUTES) add(LessonError.DurationTooLong)
        if (student.isBlank()) add(LessonError.StudentBlank)
    }
}
