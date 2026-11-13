package com.karvo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.launch

/** How far right (a share of the page width) the page has to be dragged to go back when let go. */
private const val BACK_AT = 0.33f
/** A flick to the right this fast (px per second) goes back however short it was. */
private const val FLICK = 1200f

/**
 * Swipe the page to the right to go back: it follows the finger, fading a little, and on letting go either slides
 * out and calls [onBack], or springs back. Vertical scrolling inside keeps working: only a sideways drag is taken.
 * Not for pages that already swipe sideways (the day page, the photo viewer, the calendar).
 */
@Composable
fun Modifier.swipeBack(onBack: () -> Unit): Modifier {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val back by rememberUpdatedState(onBack)
    return this
        .pointerInput(Unit) {
            val tracker = VelocityTracker()
            detectHorizontalDragGestures(
                onDragStart = { tracker.resetTracking() },
                onHorizontalDrag = { change, amount ->
                    tracker.addPosition(change.uptimeMillis, change.position)
                    change.consume()
                    scope.launch { offset.snapTo((offset.value + amount).coerceAtLeast(0f)) }
                },
                onDragEnd = {
                    val width = size.width.toFloat()
                    val gone = offset.value > width * BACK_AT || tracker.calculateVelocity().x > FLICK
                    scope.launch {
                        if (gone) {
                            offset.animateTo(width, tween(160))
                            back()
                        } else offset.animateTo(0f, tween(200))
                    }
                },
                onDragCancel = { scope.launch { offset.animateTo(0f, tween(200)) } },
            )
        }
        .graphicsLayer {
            translationX = offset.value
            alpha = 1f - 0.4f * (offset.value / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
        }
}
