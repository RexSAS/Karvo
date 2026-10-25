@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.karvo.ui.moments

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** The navigation's shared-transition scope, so a moment can fly between the day page and the viewer. */
val LocalSharedTransition = compositionLocalOf<SharedTransitionScope?> { null }

/** The animation of the screen being shown, set by each destination that takes part. */
val LocalNavAnimation = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** The same element on both screens, by [key]: during navigation it moves and resizes from one to the other. */
@Composable
fun Modifier.sharedMoment(key: String): Modifier {
    val shared = LocalSharedTransition.current ?: return this
    val animation = LocalNavAnimation.current ?: return this
    return with(shared) { this@sharedMoment.sharedElement(rememberSharedContentState(key), animation) }
}
