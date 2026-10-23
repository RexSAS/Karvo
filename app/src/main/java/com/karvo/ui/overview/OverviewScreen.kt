package com.karvo.ui.overview

import android.os.Build
import com.karvo.domain.StudentStat
import com.karvo.domain.OverviewStats
import com.karvo.domain.MonthBar
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Button
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.karvo.ui.components.ProCrown
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.karvo.R
import com.karvo.domain.Season
import com.karvo.ui.components.PaidBadge
import com.karvo.ui.components.PartlyPaidColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karvo.domain.earningsLabel
import com.karvo.domain.formatDuration
import com.karvo.domain.monthTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.karvo.domain.dayTitle
import com.karvo.ui.theme.AlpineNight
import com.karvo.ui.theme.GlassPanel
import com.karvo.ui.theme.ScreenHeader
import com.karvo.ui.theme.groupedShape
import com.karvo.ui.theme.isAlpineNight
import com.karvo.ui.theme.tileColor
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onOpenMonth: (YearMonth) -> Unit,
    onOpenSettings: () -> Unit,
    /** Opens the page listing what Karvo Pro includes. */
    onOpenPro: () -> Unit,
    /** False while another pager page is on screen: earnings hide as soon as the user swipes away. */
    isCurrentPage: Boolean = true,
    viewModel: OverviewViewModel = viewModel(factory = OverviewViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Earnings go back to hidden whenever the screen stops (app sent to background, or navigated away).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.hide() }
    LaunchedEffect(isCurrentPage) { if (!isCurrentPage) viewModel.hide() }
    val promptTitle = stringResource(R.string.show_earnings)
    val onEarningsTap: () -> Unit = when {
        state.loading -> ({})
        state.rateSet -> ({ viewModel.toggleReveal(promptTitle) })
        else -> onOpenSettings
    }

    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            ScreenHeader(
                title = stringResource(R.string.overview),
                subtitle = dayTitle(LocalDate.now()),
                trailing = {
                    Icon(
                        painterResource(R.drawable.ic_emblem),
                        contentDescription = null,
                        tint = if (isAlpineNight()) AlpineNight.Gold else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(width = 58.dp, height = 45.dp),
                    )
                },
            )
        },
    ) { padding ->
        // Still loading: show nothing rather than flashing 0m / — €.
        if (state.loading) return@Scaffold
        val totalPanel: @Composable () -> Unit = {
            GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = if (state.stats != null) 28.dp else 16.dp)) {
                    Text(
                        stringResource(R.string.total_hours).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(formatDuration(state.totalMinutes), style = MaterialTheme.typography.displaySmall)
                    // Without Pro the panel keeps the hours only: no money anywhere.
                    if (state.pro) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            earningsLabel(state.totalEarningsCents, state.revealed),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f).clickable(onClick = { if (!state.rateSet) onOpenSettings() }),
                        )
                        IconButton(onClick = onEarningsTap) {
                            Icon(
                                if (state.revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(if (state.revealed) R.string.hide_earnings else R.string.show_earnings),
                            )
                        }
                    }
                    state.seasons?.takeIf { state.pro }?.let { seasons ->
                        val current = seasons.current
                        Text(
                            stringResource(R.string.this_season, current.season.label()) + " · " +
                                formatDuration(current.minutes) + " · " + earningsLabel(current.earningsCents, state.revealed),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        seasons.previousToDateMinutes?.let { previous ->
                            Text(
                                stringResource(
                                    R.string.season_vs_last,
                                    signedDuration(current.minutes - previous),
                                    Season(current.season.startYear - 1, current.season.startMonth).label(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (state.pro && state.unpaid.minutes > 0) {
                        Text(
                            pluralStringResource(
                                R.plurals.to_be_paid,
                                state.unpaid.months,
                                state.unpaid.months,
                                formatDuration(state.unpaid.minutes),
                                earningsLabel(state.unpaid.earningsCents, state.revealed),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PartlyPaidColor,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        if (!state.pro) {
            // Not scrollable: the locked area fills the rest of the screen.
            Column(Modifier.padding(padding).fillMaxSize()) {
                totalPanel()
                ProLocked(onUnlock = onOpenPro, Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    SummaryTiles(SampleStats, Modifier.padding(top = 12.dp))
                    StatsSection(SampleStats, revealed = false, rateSet = false)
                }
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "total") { totalPanel() }
            val stats = state.stats
            if (stats == null) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.no_lessons),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                    )
                }
                return@LazyColumn
            }
            item(key = "summary") {
                // Overlaps the bottom of the glass panel.
                SummaryTiles(stats, Modifier.padding(horizontal = 16.dp).offset(y = (-18).dp))
            }
            item(key = "stats") { StatsSection(stats, state.revealed, state.rateSet) }
            state.seasons?.let { seasons ->
                item(key = "seasons") { SeasonsSection(seasons, state.revealed, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            }
            item(key = "months-header") {
                Text(
                    stringResource(R.string.months),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(state.months, key = { _, row -> row.summary.month.toString() }) { index, row ->
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .clip(groupedShape(index, state.months.size))
                        .background(tileColor())
                        .clickable { onOpenMonth(row.summary.month) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(monthTitle(row.summary.month), style = MaterialTheme.typography.titleMedium)
                            PaidBadge(row.summary.paidStatus)
                        }
                        Text(formatDuration(row.summary.totalMinutes), style = MaterialTheme.typography.titleMedium)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val n = row.summary.lessonCount
                        Text(
                            pluralStringResource(R.plurals.lesson_count, n, n),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            earningsLabel(row.earningsCents, state.revealed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                if (index < state.months.lastIndex) {
                    HorizontalDivider(Modifier.padding(horizontal = 32.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

/** Made-up statistics drawn behind the blur, so nothing real shows through. */
private val SampleStats = YearMonth.now().let { now ->
    val minutes = listOf(540L, 720L, 1260L, 1680L, 960L, 1440L)
    OverviewStats(
        lessonsThisMonth = 14,
        averageLessonMinutes = 75,
        studentCount = 9,
        lastSixMonths = minutes.mapIndexed { i, m -> MonthBar(now.minusMonths(5L - i), m, null) },
        sixMonthEarningsCents = null,
        topStudents = listOf("Marco" to 1260L, "Giulia" to 1020L, "Luca" to 840L, "Sofia" to 600L, "Elena" to 420L)
            .map { (name, m) -> StudentStat(name, m, null) },
        weekdayCounts = listOf(3, 2, 4, 3, 6, 9, 8),
    )
}

/**
 * Pro-only content drawn blurred, with edges fading out, under a "Pro plan needed" card. Android 11 and
 * older can't blur, so a near-opaque veil hides the content there. Taps anywhere open the Pro page.
 */
@Composable
private fun ProLocked(onUnlock: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val veil = MaterialTheme.colorScheme.surface.copy(alpha = if (canBlur) 0.2f else 0.92f)
    Box(modifier.fillMaxSize().clipToBounds()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(veil)
                    // Fade all four edges to transparent.
                    val fade = 0.12f
                    drawRect(Brush.verticalGradient(0f to Color.Transparent, fade to Color.Black, 1f - fade to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                    drawRect(Brush.horizontalGradient(0f to Color.Transparent, fade to Color.Black, 1f - fade to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                },
        ) {
            Column(
                Modifier
                    .wrapContentHeight(Alignment.Top, unbounded = true)
                    .blur(12.dp, BlurredEdgeTreatment.Unbounded)
                    .clearAndSetSemantics {},
            ) { content() }
        }
        Box(
            Modifier
                .matchParentSize()
                // No semantics: the Unlock button is the accessible way in.
                .pointerInput(onUnlock) { detectTapGestures { onUnlock() } },
        )
        ElevatedCard(Modifier.align(Alignment.Center).padding(24.dp)) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProCrown(Modifier.size(40.dp))
                Text(stringResource(R.string.pro_locked_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.pro_locked_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onUnlock, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.pro_unlock)) }
            }
        }
    }
}
