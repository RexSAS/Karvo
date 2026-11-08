package com.karvo.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class StartScreenTest {
    @Test fun `a fresh install welcomes`() = assertEquals(StartScreen.Welcome, startScreen(lastSeen = null, current = "3.2", hasLessons = false))
    @Test fun `an update from 3_1 (no version stored yet, lessons exist) shows what's new`() =
        assertEquals(StartScreen.WhatsNew, startScreen(lastSeen = null, current = "3.2", hasLessons = true))
    @Test fun `an update shows what's new once`() {
        assertEquals(StartScreen.WhatsNew, startScreen("3.1", "3.2", true))
        assertEquals(StartScreen.None, startScreen("3.2", "3.2", true))
    }
    @Test fun `a downgrade or an unreadable version shows nothing`() {
        assertEquals(StartScreen.None, startScreen("3.3", "3.2", true))
        assertEquals(StartScreen.None, startScreen("x", "3.2", true))
    }
}
