package com.karvo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.karvo.R
import com.karvo.ui.theme.reducedMotion

/** Kevin, the Karvo penguin, coming out of the powder snow. His drawing is made by docs/mascot/kevin.mjs. */
enum class KevinPose(@DrawableRes val head: Int) {
    /** Awake: he says hello. */
    Idle(R.drawable.kevin_head_idle),

    /** Asleep under a little snow: there is nothing here yet. */
    Sleepy(R.drawable.kevin_head_sleepy),
}

// The drawing is 600 x 580 units, with its top 44 units above zero (VIEW, RISE, BOB and SNOW_LINE in kevin.mjs).
private const val HEIGHT = 580f
private const val RISE = 400f / HEIGHT
private const val BOB = 6f / HEIGHT
private const val SNOW_LINE = (474f + 44f) / HEIGHT

/**
 * Kevin comes up out of the snow, then his head bobs a little; asleep, the z's breathe. Four still layers that only
 * slide and fade, so he costs nothing to animate. Decorative: the text next to him says what he means.
 * With animations turned off in the phone's settings he is simply there, still: Compose ends every animation at once.
 */
@Composable
fun Kevin(pose: KevinPose, modifier: Modifier = Modifier) {
    // With animations off he is up from the first frame (no frame without a head).
    val still = reducedMotion()
    val rise = remember { Animatable(if (still) 0f else 1f) }
    LaunchedEffect(Unit) { rise.animateTo(0f, tween(800, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) }
    val loop = rememberInfiniteTransition(label = "kevin")
    val bob = loop.animateFloat(
        0f, 0f,
        infiniteRepeatable(keyframes { durationMillis = 3800; 0f at 0 using EaseInOut; 1f at 1900 using EaseInOut }),
        label = "bob",
    )
    val breath = loop.animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 3200; 1f at 0 using EaseInOut; .25f at 1600 using EaseInOut }),
        label = "breath",
    )
    Box(modifier.aspectRatio(600f / HEIGHT)) {
        val layer = Modifier.matchParentSize()
        Image(painterResource(R.drawable.kevin_back), contentDescription = null, modifier = layer)
        // Below the snow line the head is hidden: by the snow in front, and by nothing at all under the mound.
        Box(layer.drawWithContent { clipRect(bottom = size.height * SNOW_LINE) { this@drawWithContent.drawContent() } }) {
            Image(
                painterResource(pose.head),
                contentDescription = null,
                modifier = Modifier.matchParentSize().graphicsLayer { translationY = (rise.value * RISE - bob.value * BOB) * size.height },
            )
        }
        Image(painterResource(R.drawable.kevin_front), contentDescription = null, modifier = layer)
        if (pose == KevinPose.Sleepy) {
            Image(painterResource(R.drawable.kevin_zzz), contentDescription = null, modifier = layer.graphicsLayer { alpha = breath.value })
        }
    }
}

/**
 * An empty page: Kevin asleep in the snow, and one line that says what is missing.
 * On a page with a fixed height he shrinks when there is little room (a phone on its side); in a list he has his full size.
 */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val shrink = constraints.hasBoundedHeight
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Kevin(KevinPose.Sleepy, (if (shrink) Modifier.weight(1f, fill = false) else Modifier).widthIn(max = 200.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
