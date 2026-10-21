package com.masterski.ui.overview

import androidx.compose.ui.res.stringResource
import com.masterski.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.masterski.ui.components.LessonRow
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDetailScreen(
    month: YearMonth,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: MonthDetailViewModel = viewModel(key = "month-$month", factory = MonthDetailViewModel.factory(month)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Screen-local and not saved; hidden each time the screen opens and whenever it stops.
    var revealed by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { revealed = false }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(monthTitle(month)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ElevatedCard(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.total_hours), style = MaterialTheme.typography.labelLarge)
                    Text(formatDuration(state.totalMinutes), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.earnings), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            earningsLabel(state.earningsCents, revealed),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { revealed = !revealed }) {
                            Icon(
                                if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(if (revealed) R.string.hide_earnings else R.string.show_earnings),
                            )
                        }
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.lessons, key = { it.id }) { lesson ->
                    LessonRow(lesson, onClick = { onEdit(lesson.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}
