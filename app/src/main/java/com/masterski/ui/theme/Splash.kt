package com.masterski.ui.theme

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreenViewProvider

/** Upper bound on how long the splash waits for the theme setting, so a slow disk never blocks the app. */
const val SPLASH_MAX_WAIT_MS = 1_500L

/** Length of the animated splash logo (values-v31/themes.xml); older Android shows a still logo. */
const val SPLASH_ANIMATION_MS = 800L

/**
 * The splash stays until its animation has played ([minShowMs]) and the theme is known (so the first frame has
 * the right theme), but never past [SPLASH_MAX_WAIT_MS].
 */
fun shouldKeepSplash(themeLoaded: Boolean, elapsedMs: Long, minShowMs: Long): Boolean =
    elapsedMs < minShowMs || (!themeLoaded && elapsedMs < SPLASH_MAX_WAIT_MS)

/** Exit: the logo grows a little while the whole splash fades into the app. */
fun animateSplashExit(provider: SplashScreenViewProvider) {
    val splash = provider.view
    val icon: View? = runCatching { provider.iconView }.getOrNull()
    val animators = mutableListOf(ObjectAnimator.ofFloat(splash, View.ALPHA, 1f, 0f))
    if (icon != null) {
        animators += ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.15f)
        animators += ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.15f)
    }
    AnimatorSet().apply {
        duration = 300
        playTogether(animators.toList())
        doOnEnd { provider.remove() }
        start()
    }
}
