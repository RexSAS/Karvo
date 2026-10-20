package com.masterski.domain

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class LessonValidationTest {
    @Test fun ok() = assertEquals(emptySet<LessonError>(), validateLesson("Anna", 1, 30))
    @Test fun blankStudent() = assertEquals(setOf(LessonError.StudentBlank), validateLesson("   ", 1, 0))
    @Test fun zero() = assertEquals(setOf(LessonError.DurationZero), validateLesson("A", 0, 0))
    @Test fun tooLong() = assertEquals(setOf(LessonError.DurationTooLong), validateLesson("A", 24, 1))
    @Test fun exactly24h() = assertEquals(emptySet<LessonError>(), validateLesson("A", 24, 0))
    @Test fun minutes60() = assertTrue(LessonError.MinutesOver59 in validateLesson("A", 1, 60))
}
