package com.karvo.ui.overview

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.WindowInsets
import com.karvo.ui.theme.underBars
import com.karvo.ui.theme.LocalTabBarSpace
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.karvo.R
import com.karvo.domain.Season
import com.karvo.ui.components.PaidBadge
import com.karvo.ui.components.EmptyState
import com.karvo.ui.components.partlyPaidColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.remember
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
import com.karvo.domain.dayTitle
import com.karvo.ui.theme.GlassPanel
import com.karvo.ui.theme.HorizonLine
import com.karvo.ui.theme.KarvoMotion
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.karvo.ui.theme.bigDuration
import com.karvo.ui.theme.rowNumber
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
    onOpenStudent: (String) -> Unit,
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

    val tabBar = LocalTabBarSpace.current
    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        contentWindowInsets = WindowInsets(0), // the header pads itself; the list runs under the tab bar
        topBar = {
            ScreenHeader(
                title = stringResource(R.string.overview),
                subtitle = dayTitle(LocalDate.now()),
                afterTitle = {
                    Icon(
                        painterResource(R.drawable.ic_emblem),
                        contentDescription = null,
                        // Alpine Night shows the logo in its own colours; Classic tints it with the theme.
                        tint = if (isAlpineNight()) Color.Unspecified else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(width = 36.dp, height = 28.dp),
                    )
                },
                trailing = { if (!state.loading && state.seasons != null) ScopePicker(state, viewModel::show) },
            )
        },
    ) { padding ->
        // Still loading: show nothing rather than flashing 0m / — €.
        if (state.loading) return@Scaffold
        // A glint runs along the horizon line each time the earnings come out.
        val glint = remember { Animatable(1f) }
        LaunchedEffect(state.revealed) {
            if (state.revealed) { glint.snapTo(0f); glint.animateTo(1f, tween(KarvoMotion.Long, easing = KarvoMotion.Ease)) }
        }
        val totalPanel: @Composable () -> Unit = {
            GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = if (state.stats != null) 28.dp else 16.dp)) {
                    Text(
                        (state.scopeSeason?.let { stringResource(R.string.overview_scope_season, it.label()) } ?: stringResource(R.string.total_hours)).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        bigDuration(formatDuration(state.totalMinutes), MaterialTheme.colorScheme.onSurfaceVariant),
                        style = if (isAlpineNight()) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displaySmall,
                        maxLines = 1,
                    )
                    HorizonLine(Modifier.padding(end = 10.dp, top = 2.dp, bottom = 6.dp), glint = { glint.value })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            earningsLabel(state.totalEarningsCents, state.revealed),
                            style = if (isAlpineNight()) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
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
                    state.seasons?.takeIf { state.scope !is OverviewScope.Past }?.let { seasons ->
                        val current = seasons.current
                        // Lifetime: this season as one line. This season: the big number above already is it.
                        if (state.scope == OverviewScope.Lifetime) Text(
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
                    if (state.unpaid.minutes > 0) {
                        Text(
                            pluralStringResource(
                                R.plurals.to_be_paid,
                                state.unpaid.months,
                                state.unpaid.months,
                                formatDuration(state.unpaid.minutes),
                                earningsLabel(state.unpaid.earningsCents, state.revealed),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = partlyPaidColor(),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = underBars(padding, extra = 24.dp, bottomBar = tabBar)) {
            item(key = "total") { totalPanel() }
            val stats = state.stats
            if (stats == null) {
                item(key = "empty") {
                    EmptyState(stringResource(R.string.no_lessons), Modifier.fillMaxWidth().padding(top = 32.dp))
                }
                return@LazyColumn
            }
            item(key = "summary") {
                // Overlaps the bottom of the glass panel.
                SummaryTiles(
                    stats, Modifier.padding(horizontal = 16.dp).offset(y = (-18).dp),
                    lessonsInScope = if (state.scope == OverviewScope.ThisSeason) null else state.months.sumOf { it.summary.lessonCount },
                )
            }
            item(key = "stats") { StatsSection(stats, state.revealed, state.rateSet, onOpenStudent) }
            // Every season side by side: only for Lifetime (the picker shows one season at a time).
            state.seasons?.takeIf { state.scope == OverviewScope.Lifetime }?.let { seasons ->
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
                        Text(formatDuration(row.summary.totalMinutes), style = MaterialTheme.typography.rowNumber)
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

/** The pill in the corner: which season Overview counts (this one, a past one) or Lifetime. */
@Composable
private fun ScopePicker(state: OverviewUiState, onChoose: (OverviewScope) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val lifetime = stringResource(R.string.overview_lifetime)
    val label = state.scopeSeason?.label() ?: lifetime
    val describe = stringResource(R.string.overview_scope_a11y, label)
    Box {
        Row(
            Modifier
                .clip(CircleShape)
                .background(tileColor())
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(onClickLabel = describe) { open = true }
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                .semantics { contentDescription = describe },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val current = state.seasons?.current?.season
            fun choose(scope: OverviewScope) { open = false; onChoose(scope) }
            val pick = @Composable { text: String, scope: OverviewScope ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { choose(scope) },
                    trailingIcon = { if (state.scope == scope) Icon(Icons.Filled.Check, contentDescription = null) },
                )
            }
            if (current != null) pick(stringResource(R.string.this_season, current.label()), OverviewScope.ThisSeason)
            state.pastSeasons.forEach { pick(it.label(), OverviewScope.Past(it)) }
            pick(lifetime, OverviewScope.Lifetime)
        }
    }
}
