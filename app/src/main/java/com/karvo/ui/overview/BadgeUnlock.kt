package com.karvo.ui.overview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.karvo.R
import com.karvo.domain.Badge
import com.karvo.domain.formatDuration
import com.karvo.ui.components.drawBadge
import com.karvo.ui.components.titleRes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** The unlock moment over the whole app: rays, the badge popping in with a burst of sparkles, then its name. */
@Composable
fun BadgeUnlock(badge: Badge, onDone: () -> Unit, onSeeBadges: () -> Unit) {
    BackHandler(onBack = onDone)
    val haptics = LocalHapticFeedback.current
    val gold = Color(0xFFF4C06A) // the summit gold, always on the dark backdrop
    val scrim = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val glint = remember { Animatable(-1f) }
    val words = remember { Animatable(0f) }
    LaunchedEffect(badge) {
        launch { scrim.animateTo(1f, tween(300)) }
        delay(250)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
        launch { burst.animateTo(1f, tween(1100)) }
        launch { delay(250); words.animateTo(1f, tween(400)) }
        delay(500)
        repeat(2) { glint.snapTo(0f); glint.animateTo(1f, tween(800)); delay(500) }
        glint.snapTo(-1f)
    }
    val spin by rememberInfiniteTransition(label = "rays").animateFloat(
        0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart), label = "spin",
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = scrim.value }
            .background(Color(0xE6050913))
            // Swallows taps, so nothing underneath reacts while the moment plays.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                // Rays and glow behind, a ring of sparkles flying out.
                Canvas(Modifier.size(300.dp).graphicsLayer { alpha = pop.value.coerceIn(0f, 1f) }) {
                    val c = center
                    val r = size.minDimension / 2
                    for (i in 0 until 16) rotate(spin + i * 22.5f, c) {
                        drawPath(Path().apply { moveTo(c.x, c.y); lineTo(c.x + r, c.y - r * 0.07f); lineTo(c.x + r, c.y + r * 0.07f); close() },
                            Brush.linearGradient(listOf(gold.copy(alpha = 0.35f), gold.copy(alpha = 0f)), c, Offset(c.x + r, c.y)))
                    }
                    drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = 0.35f), gold.copy(alpha = 0f)), c, r * 0.8f), r * 0.8f, c)
                    val p = burst.value
                    if (p in 0.01f..0.99f) for (i in 0 until 14) {
                        val a = i * Math.PI * 2 / 14 + 0.2
                        val d = r * (0.25f + 0.75f * p)
                        val at = Offset(c.x + d * cos(a).toFloat(), c.y + d * sin(a).toFloat())
                        val k = r * 0.05f * (1 - p)
                        val w = k * 0.25f
                        drawPath(Path().apply {
                            moveTo(at.x, at.y - k); lineTo(at.x + w, at.y - w); lineTo(at.x + k, at.y); lineTo(at.x + w, at.y + w)
                            lineTo(at.x, at.y + k); lineTo(at.x - w, at.y + w); lineTo(at.x - k, at.y); lineTo(at.x - w, at.y - w); close()
                        }, if (i % 2 == 0) Color.White else gold, alpha = 1 - p)
                    }
                }
                Canvas(
                    Modifier.size(230.dp).graphicsLayer {
                        scaleX = 0.2f + 0.8f * pop.value; scaleY = scaleX
                        rotationZ = (1 - pop.value) * -14f
                        alpha = pop.value.coerceIn(0f, 1f)
                    },
                ) { drawBadge(badge, Color.Gray, glint = glint.value, spin = spin) }
            }
            Column(
                Modifier.graphicsLayer { alpha = words.value; translationY = (1 - words.value) * 24.dp.toPx() }
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.badge_unlocked).uppercase(), style = MaterialTheme.typography.labelLarge, color = gold)
                Text(stringResource(badge.titleRes()), style = MaterialTheme.typography.displaySmall, color = Color.White, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.badge_unlocked_at, formatDuration(badge.minutes)),
                    style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp).graphicsLayer { alpha = words.value },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onSeeBadges) { Text(stringResource(R.string.badge_see_all), color = Color.White) }
            Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF1B2433))) {
                Text(stringResource(R.string.badge_unlocked_ok))
            }
        }
    }
}
