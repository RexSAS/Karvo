package com.karvo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.karvo.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen "Karvo Pro unlocked" moment: the crown pops in, a gold ring and sparks burst out,
 * then it closes by itself (or on tap). Follows the system animation scale, so "remove animations" skips it.
 */
@Composable
fun ProUnlockedCelebration(onDone: () -> Unit) {
    val crown = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }
    val fadeOut = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { crown.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        launch { burst.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { text.animateTo(1f, tween(400, delayMillis = 300)) }
        delay(1900)
        fadeOut.animateTo(0f, tween(300))
        onDone()
    }
    val gold = Color(0xFFFFC107)
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().alpha(fadeOut.value).background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(280.dp)) {
                val t = burst.value
                val maxR = size.minDimension / 2
                drawCircle(gold.copy(alpha = 1f - t), radius = maxR * t, style = Stroke(width = 6.dp.toPx() * (1f - t) + 1f))
                repeat(12) { i ->
                    val angle = Math.toRadians(i * 30.0)
                    val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val start = center + dir * (maxR * 0.35f + maxR * 0.4f * t)
                    val end = center + dir * (maxR * 0.45f + maxR * 0.55f * t)
                    drawLine(gold.copy(alpha = 1f - t), start, end, strokeWidth = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProCrown(Modifier.size(96.dp).scale(crown.value), contentDescription = null)
                Text(
                    stringResource(R.string.pro_unlocked),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.alpha(text.value),
                )
            }
        }
    }
}
