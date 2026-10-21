package com.masterski.ui.overview

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.masterski.R
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
import com.masterski.domain.earningsLabel
import com.masterski.domain.formatDuration
import com.masterski.domain.monthTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.masterski.domain.dayTitle
import com.masterski.ui.theme.AlpineNight
import com.masterski.ui.theme.GlassPanel
import com.masterski.ui.theme.ScreenHeader
import com.masterski.ui.theme.groupedShape
import com.masterski.ui.theme.isAlpineNight
import com.masterski.ui.theme.tileColor
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onOpenMonth: (YearMonth) -> Unit,
    onOpenSettings: () -> Unit,
    /** False while another pager page is on screen: earnings hide as soon as the user swipes away. */
    isCurrentPage: Boolean = true,
    viewModel: OverviewViewModel = viewModel(factory = OverviewViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Earnings go back to hidden whenever the screen stops (app sent to background, or navigated away).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.hide() }
    LaunchedEffect(isCurrentPage) { if (!isCurrentPage) viewModel.hide() }
    val onEarningsTap: () -> Unit = when {
        state.loading -> ({})
        state.rateSet -> viewModel::toggleReveal
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
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "total") {
                GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = if (state.stats != null) 28.dp else 16.dp)) {
                        Text(
                            stringResource(R.string.total_hours).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(formatDuration(state.totalMinutes), style = MaterialTheme.typography.displaySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    }
                }
            }
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
                        Text(monthTitle(row.summary.month), style = MaterialTheme.typography.titleMedium)
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
