package com.karvo.ui

import androidx.annotation.StringRes
import com.karvo.R
import com.karvo.domain.LessonError

@StringRes
fun LessonError.messageRes(): Int = when (this) {
    LessonError.StudentBlank -> R.string.error_student_blank
    LessonError.DurationZero -> R.string.error_duration_zero
    LessonError.DurationTooLong -> R.string.error_duration_too_long
    LessonError.MinutesOver59 -> R.string.error_minutes_over_59
}
