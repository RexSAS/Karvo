package com.masterski.ui.overview

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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun monthTitle(month: YearMonth): String =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(month)

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
        topBar = { CenterAlignedTopAppBar(title = { Text("Overview") }) },
    ) { padding ->
        // Still loading: show nothing rather than flashing 0m / — €.
        if (state.loading) return@Scaffold
        Column(Modifier.padding(padding).fillMaxSize()) {
            ElevatedCard(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total hours", style = MaterialTheme.typography.labelLarge)
                    Text(formatDuration(state.totalMinutes), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Earnings",
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
                                contentDescription = if (state.revealed) "Hide earnings" else "Show earnings",
                            )
                        }
                    }
                }
            }
            if (state.months.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No lessons yet.") }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.months, key = { it.summary.month.toString() }) { row ->
                        Column(Modifier.fillMaxWidth().clickable { onOpenMonth(row.summary.month) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(monthTitle(row.summary.month), style = MaterialTheme.typography.titleMedium)
                                Text(formatDuration(row.summary.totalMinutes), style = MaterialTheme.typography.titleMedium)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val n = row.summary.lessonCount
                                Text(
                                    if (n == 1) "1 lesson" else "$n lessons",
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
