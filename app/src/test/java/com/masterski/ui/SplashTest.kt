package com.masterski.ui

import com.masterski.ui.theme.SPLASH_MAX_WAIT_MS
import com.masterski.ui.theme.shouldKeepSplash
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashTest {
    @Test fun keepsSplashUntilThemeIsKnown() =
        assertTrue(shouldKeepSplash(themeLoaded = false, elapsedMs = 900, minShowMs = 800))

    @Test fun releasesSplashOnceThemeIsKnownAndAnimationPlayed() =
        assertFalse(shouldKeepSplash(themeLoaded = true, elapsedMs = 800, minShowMs = 800))

    @Test fun letsTheAnimationFinishEvenIfThemeIsReadyEarly() =
        assertTrue(shouldKeepSplash(themeLoaded = true, elapsedMs = 100, minShowMs = 800))

    @Test fun noMinimumWhenThereIsNoAnimation() =
        assertFalse(shouldKeepSplash(themeLoaded = true, elapsedMs = 100, minShowMs = 0))

    @Test fun neverHangsIfSettingsAreSlow() =
        assertFalse(shouldKeepSplash(themeLoaded = false, elapsedMs = SPLASH_MAX_WAIT_MS, minShowMs = 800))
}
