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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterski.domain.earningsLabel
import com.masterski.domain.formatDuration
import com.masterski.domain.monthTitle
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onOpenMonth: (YearMonth) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: OverviewViewModel = viewModel(factory = OverviewViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Earnings go back to hidden whenever the screen stops (app sent to background, or navigated away).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.hide() }
    val onEarningsTap: () -> Unit = when {
        state.loading -> ({})
        state.rateSet -> viewModel::toggleReveal
        else -> onOpenSettings
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.overview)) }) },
    ) { padding ->
        // Still loading: show nothing rather than flashing 0m / — €.
        if (state.loading) return@Scaffold
        Column(Modifier.padding(padding).fillMaxSize()) {
            ElevatedCard(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.total_hours), style = MaterialTheme.typography.labelLarge)
                    Text(formatDuration(state.totalMinutes), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.earnings),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            earningsLabel(state.totalEarningsCents, state.revealed),
                            style = MaterialTheme.typography.headlineMedium,
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
            if (state.months.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_lessons)) }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    state.stats?.let { stats ->
                        item(key = "stats") {
                            StatsSection(stats, state.revealed, state.rateSet, Modifier.padding(vertical = 16.dp))
                        }
                    }
                    item(key = "months-header") {
                        Text(
                            stringResource(R.string.months),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(state.months, key = { it.summary.month.toString() }) { row ->
                        Column(Modifier.fillMaxWidth().clickable { onOpenMonth(row.summary.month) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
