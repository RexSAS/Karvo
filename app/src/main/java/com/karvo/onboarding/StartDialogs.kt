package com.karvo.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.karvo.BuildConfig
import com.karvo.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ease = CubicBezierEasing(.2f, .8f, .2f, 1f)

/** Fades and slides its content up after [delayMs]: the staggered entrance of the start screens. */
@Composable
private fun Entrance(delayMs: Int, content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(450, easing = Ease)) }
    androidx.compose.foundation.layout.Box(Modifier.graphicsLayer { alpha = p.value; translationY = (1 - p.value) * 14.dp.toPx() }) { content() }
}

/** The Karvo logo as on the splash: the peaks rise from the ground, then the gold sun pops. */
@Composable
fun AnimatedLogo(modifier: Modifier = Modifier) {
    val far = remember { Animatable(0f) }
    val near = remember { Animatable(0f) }
    val sun = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { far.animateTo(1f, tween(500, easing = Ease)) }
        launch { delay(150); near.animateTo(1f, tween(500, easing = Ease)) }
        delay(500); sun.animateTo(1f, tween(300, easing = Ease))
    }
    Canvas(modifier.semantics { contentDescription = "Karvo" }) {
        // Logo drawn in its own 72×54 box (the splash viewBox 20 26 72 54), scaled to fit.
        val k = minOf(size.width / 72f, size.height / 54f)
        fun pt(x: Float, y: Float) = Offset((x - 20) * k, (y - 26) * k)
        fun peak(a: Offset, b: Offset, c: Offset, color: Color, rise: Float) =
            scale(1f, rise, pivot = Offset(0f, c.y)) { drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }, color) }
        val c = pt(38f, 40f)
        scale(sun.value, pivot = c) { drawCircle(Color(0xFFE8B964), 5 * k, c) }
        peak(pt(72f, 48f), pt(86f, 76f), pt(58f, 76f), Color(0xFFE3F2FD), far.value)
        peak(pt(54f, 30f), pt(82f, 76f), pt(26f, 76f), Color.White, near.value)
    }
}

/** First launch: the animated logo, a welcome, and the quick tour or Skip. */
@Composable
fun WelcomeDialog(onTour: () -> Unit, onSkip: () -> Unit) {
    Dialog(onDismissRequest = onSkip) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedLogo(Modifier.size(width = 120.dp, height = 90.dp))
                Entrance(500) { Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) }
                Entrance(580) { Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Entrance(660) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        TextButton(onClick = onSkip) { Text(stringResource(R.string.tour_skip)) }
                        Button(onClick = onTour) { Text(stringResource(R.string.welcome_tour)) }
                    }
                }
            }
        }
    }
}

/** Once after an update: what changed in this version, one line after the other. */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.whats_new_title, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.headlineSmall)
                stringArrayResource(R.array.whats_new_items).forEachIndexed { i, item ->
                    Entrance(150 + i * 90) { Text("•  $item", style = MaterialTheme.typography.bodyMedium) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
                }
            }
        }
    }
}
