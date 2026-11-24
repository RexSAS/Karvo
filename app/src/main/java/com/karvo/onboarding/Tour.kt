package com.karvo.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.BiasAlignment
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karvo.R
import kotlinx.coroutines.delay

/** The buttons the quick tour points at; screens mark them with [tourTarget]. */
enum class TourTarget { AddLesson, Search, Calendar, Camera, Season, Badges, Students, Rate, Backups, LinkedPcs, Report }

/** One step: the home tab it lives on (0 lessons, 1 overview, 2 settings), what it highlights and what it says. */
data class TourStep(val target: TourTarget, val page: Int, @StringRes val title: Int, @StringRes val body: Int)

val TOUR = listOf(
    TourStep(TourTarget.AddLesson, 0, R.string.tour_add_title, R.string.tour_add_body),
    TourStep(TourTarget.Search, 0, R.string.tour_search_title, R.string.tour_search_body),
    TourStep(TourTarget.Calendar, 0, R.string.tour_calendar_title, R.string.tour_calendar_body),
    TourStep(TourTarget.Camera, 0, R.string.tour_camera_title, R.string.tour_camera_body),
    TourStep(TourTarget.Season, 1, R.string.tour_season_title, R.string.tour_season_body),
    TourStep(TourTarget.Badges, 1, R.string.tour_badges_title, R.string.tour_badges_body),
    TourStep(TourTarget.Students, 1, R.string.tour_students_title, R.string.tour_students_body),
    TourStep(TourTarget.Rate, 2, R.string.tour_settings_title, R.string.tour_settings_body),
    TourStep(TourTarget.Backups, 2, R.string.tour_backups_title, R.string.tour_backups_body),
    TourStep(TourTarget.LinkedPcs, 2, R.string.tour_pc_title, R.string.tour_pc_body),
    TourStep(TourTarget.Report, 2, R.string.tour_report_title, R.string.tour_report_body),
)

class TourState {
    var index by mutableStateOf<Int?>(null)
        private set
    /** Each target's real position, also while it is off screen (not clipped to the screen). */
    val bounds = mutableStateMapOf<TourTarget, Rect>()
    val current: TourStep? get() = index?.let(TOUR::getOrNull)
    /** False while the step's tab and scroll are still moving: the highlight and the card wait for the target to rest. */
    var settled by mutableStateOf(false)
    /** True when the current step is on another tab than the step before (the pager must settle first). */
    var pageChanged = true
        private set
    fun start() { pageChanged = true; settled = false; index = 0 }
    fun next() {
        val from = current?.page
        settled = false
        index = index?.let { if (it + 1 < TOUR.size) it + 1 else null }
        pageChanged = current?.page != from
    }
    fun stop() { index = null }
}

val LocalTour = staticCompositionLocalOf<TourState?> { null }

/** Marks a button for the tour: its position is recorded, and it is scrolled into view when its step comes. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tourTarget(target: TourTarget): Modifier = composed {
    val tour = LocalTour.current ?: return@composed this
    val bring = remember { BringIntoViewRequester() }
    val active = tour.current?.target == target
    var size by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    // The lists run under the floating tab bar and the screen title, so the target is scrolled well clear of both
    // (scrolling up, e.g. to the rates from "Show the tour again" low in Settings), with room for the ring.
    val statusBar = WindowInsets.statusBars.getTop(density)
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        if (tour.pageChanged) delay(350) // after the pager has settled on the step's tab, or the pager itself would be scrolled
        val top = statusBar + with(density) { 110.dp.toPx() }
        val bottom = with(density) { 140.dp.toPx() }
        runCatching { bring.bringIntoView(Rect(0f, -top, size.width, size.height + bottom)) } // returns once scrolled
        tour.settled = true
    }
    bringIntoViewRequester(bring).onGloballyPositioned {
        size = Size(it.size.width.toFloat(), it.size.height.toFloat())
        tour.bounds[target] = Rect(it.positionInRoot(), size)
    }
}

/**
 * Dims the app except the current target, with the step's text, Next and Skip; it switches to the step's tab itself.
 * The highlight and the card move only once the target rests ([TourState.settled]): while a tab slides or a page
 * scrolls the highlight fades out and comes back on the target, so it never wanders over the page in between.
 */
@Composable
fun TourOverlay(tour: TourState, onShowPage: (Int) -> Unit) {
    val step = tour.current ?: return
    LaunchedEffect(step) { onShowPage(step.page) }
    // A target that never shows (not laid out) must not hold the tour forever.
    LaunchedEffect(step) { delay(1200); tour.settled = true }
    val density = LocalDensity.current
    val screenH = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val pad = with(density) { 8.dp.toPx() }
    val shown = rememberSaveable { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (shown.value) 1f else 0f, tween(250), label = "tourFade")
    LaunchedEffect(Unit) { shown.value = true }
    val target = if (tour.settled) tour.bounds[step.target] else null
    val holeAlpha = animateFloatAsState(if (target != null) 1f else 0f, tween(if (target != null) 260 else 140), label = "tourHoleAlpha")
    // Glides from one resting target to the next; after a fade (tab change or scroll) it reappears in place instead.
    val hole = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    LaunchedEffect(target) {
        val to = target ?: return@LaunchedEffect
        if (holeAlpha.value < 0.3f) hole.snapTo(to) else hole.animateTo(to, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow))
    }
    // The ring grows and fades in and out, so each pulse starts from nothing instead of snapping back.
    val pulse = rememberInfiniteTransition(label = "tourPulse").animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "tourPulseP")
    val gold = MaterialTheme.colorScheme.tertiary
    var atTop by remember { mutableStateOf(false) }
    if (target != null) atTop = target.center.y > screenH / 2 // the card goes on the side away from the resting target
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.pointerInput(step) { detectTapGestures { } }) { // the app underneath stays untouchable
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.72f))
            val a = holeAlpha.value
            if (a > 0f) {
                val r = hole.value
                val tl = Offset(r.left - pad, r.top - pad)
                val sz = Size(r.width + 2 * pad, r.height + 2 * pad)
                drawRoundRect(Color.Black.copy(alpha = a), tl, sz, CornerRadius(pad * 2), blendMode = BlendMode.DstOut)
                val p = pulse.value
                val grow = p * pad * 1.5f
                drawRoundRect(gold.copy(alpha = 0.9f * sin(PI.toFloat() * p) * a), Offset(tl.x - grow, tl.y - grow), Size(sz.width + 2 * grow, sz.height + 2 * grow),
                    CornerRadius(pad * 2 + grow), style = Stroke(2.dp.toPx()))
            }
        }
        val cardBias by animateFloatAsState(if (atTop) -1f else 1f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow), label = "tourCardSide")
        // One card for the whole tour: only its text crossfades, and its height follows.
        Card(
            Modifier.align(BiasAlignment(0f, cardBias)).safeDrawingPadding().padding(20.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AnimatedContent(
                tour.index ?: 0,
                transitionSpec = { (fadeIn(tween(220, 90)) togetherWith fadeOut(tween(90))) using SizeTransform(clip = false) },
                label = "tourCard",
            ) { i ->
                val s = TOUR[i]
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${i + 1}/${TOUR.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(s.title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(s.body), style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = tour::stop) { Text(stringResource(R.string.tour_skip)) }
                        Button(onClick = tour::next) { Text(stringResource(if (i == TOUR.lastIndex) R.string.tour_done else R.string.tour_next)) }
                    }
                }
            }
        }
    }
}
