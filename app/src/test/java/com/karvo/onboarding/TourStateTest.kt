package com.karvo.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TourStateTest {
    @Test fun walksEveryStepThenEnds() {
        val t = TourState()
        assertNull(t.current)
        t.start()
        val seen = mutableListOf<TourTarget>()
        while (t.current != null) { seen += t.current!!.target; t.next() }
        assertEquals(TourTarget.entries.toList(), seen)
    }

    @Test fun skipEndsAtOnce() {
        val t = TourState().apply { start(); next(); stop() }
        assertNull(t.current)
    }

    @Test fun lessonStepsOnLessonsTabSettingsStepsOnSettingsTab() {
        assertEquals(listOf(0, 0, 0, 2, 2, 2), TOUR.map { it.page })
    }
}
