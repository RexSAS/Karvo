package com.karvo.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karvo.R
import kotlinx.coroutines.delay

/** The buttons the quick tour points at; screens mark them with [tourTarget]. */
enum class TourTarget { AddLesson, Calendar, Camera, Rate, Backups, LinkedPcs }

/** One step: the home tab it lives on (0 lessons, 2 settings), what it highlights and what it says. */
data class TourStep(val target: TourTarget, val page: Int, @StringRes val title: Int, @StringRes val body: Int)

val TOUR = listOf(
    TourStep(TourTarget.AddLesson, 0, R.string.tour_add_title, R.string.tour_add_body),
    TourStep(TourTarget.Calendar, 0, R.string.tour_calendar_title, R.string.tour_calendar_body),
    TourStep(TourTarget.Camera, 0, R.string.tour_camera_title, R.string.tour_camera_body),
    TourStep(TourTarget.Rate, 2, R.string.tour_settings_title, R.string.tour_settings_body),
    TourStep(TourTarget.Backups, 2, R.string.tour_backups_title, R.string.tour_backups_body),
    TourStep(TourTarget.LinkedPcs, 2, R.string.tour_pc_title, R.string.tour_pc_body),
)

class TourState {
    var index by mutableStateOf<Int?>(null)
        private set
    val bounds = mutableStateMapOf<TourTarget, Rect>()
    val current: TourStep? get() = index?.let(TOUR::getOrNull)
    fun start() { index = 0 }
    fun next() { index = index?.let { if (it + 1 < TOUR.size) it + 1 else null } }
    fun stop() { index = null }
}

val LocalTour = staticCompositionLocalOf<TourState?> { null }

/** Marks a button for the tour: its position is recorded, and it is scrolled into view when its step comes. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tourTarget(target: TourTarget): Modifier = composed {
    val tour = LocalTour.current ?: return@composed this
    val bring = remember { BringIntoViewRequester() }
    val active = tour.current?.target == target
    LaunchedEffect(active) { if (active) { delay(350); runCatching { bring.bringIntoView() } } }
    bringIntoViewRequester(bring).onGloballyPositioned { tour.bounds[target] = it.boundsInRoot() }
}

/** Dims the app except the current target, with the step's text, Next and Skip; it switches to the step's tab itself. */
@Composable
fun TourOverlay(tour: TourState, onShowPage: (Int) -> Unit) {
    val step = tour.current ?: return
    LaunchedEffect(step) { onShowPage(step.page) }
    val density = LocalDensity.current
    val screenH = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val rect = tour.bounds[step.target]
    val pad = with(density) { 8.dp.toPx() }
    // The highlight glides from one target to the next and pulses a gold ring; the whole overlay fades in.
    val shown = rememberSaveable { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (shown.value) 1f else 0f, tween(250), label = "tourFade")
    LaunchedEffect(Unit) { shown.value = true }
    val hole by animateRectAsState(rect ?: Rect(Offset(0f, 0f), Size.Zero), tween(400, easing = FastOutSlowInEasing), label = "tourHole")
    val pulse = rememberInfiniteTransition(label = "tourPulse").animateFloat(0f, 1f, infiniteRepeatable(tween(1600)), label = "tourPulseP")
    val gold = MaterialTheme.colorScheme.tertiary
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.pointerInput(step) { detectTapGestures { } }) { // the app underneath stays untouchable
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.72f))
            if (rect != null) {
                val tl = Offset(hole.left - pad, hole.top - pad)
                val sz = Size(hole.width + 2 * pad, hole.height + 2 * pad)
                drawRoundRect(Color.Transparent, tl, sz, CornerRadius(pad * 2), blendMode = BlendMode.Clear)
                val grow = pulse.value * pad * 1.5f
                drawRoundRect(gold.copy(alpha = 0.9f * (1 - pulse.value)), Offset(tl.x - grow, tl.y - grow), Size(sz.width + 2 * grow, sz.height + 2 * grow),
                    CornerRadius(pad * 2 + grow), style = Stroke(2.dp.toPx()))
            }
        }
        val atTop = rect != null && rect.center.y > screenH / 2 // the card goes on the side away from the target
        AnimatedContent(
            tour.index, Modifier.align(if (atTop) Alignment.TopCenter else Alignment.BottomCenter),
            transitionSpec = { (fadeIn(tween(300, 100)) + slideInVertically(tween(350, 100)) { it / 6 }) togetherWith fadeOut(tween(120)) },
            label = "tourCard",
        ) { _ ->
        Card(
            Modifier.safeDrawingPadding().padding(20.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${(tour.index ?: 0) + 1}/${TOUR.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(step.title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(step.body), style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = tour::stop) { Text(stringResource(R.string.tour_skip)) }
                    Button(onClick = tour::next) { Text(stringResource(if (tour.index == TOUR.lastIndex) R.string.tour_done else R.string.tour_next)) }
                }
            }
        }
        }
    }
}
