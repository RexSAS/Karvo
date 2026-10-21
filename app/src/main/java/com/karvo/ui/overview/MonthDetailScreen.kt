package com.karvo.ui.overview

import androidx.compose.ui.res.stringResource
import com.karvo.R
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.clickable
import com.karvo.domain.PaidStatus
import com.karvo.domain.pickerMillisToLocalDate
import com.karvo.domain.toPickerMillis
import com.karvo.ui.components.PartlyPaidColor
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.karvo.ui.theme.AlpineTile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.karvo.ui.components.LessonRow
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
    // Hidden each time the screen opens and whenever it stops; showing may ask for fingerprint/PIN.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.hide() }
    val promptTitle = stringResource(R.string.show_earnings)
    var showPaidPicker by remember { mutableStateOf(false) }
    var showUnpaidConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
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
            AlpineTile(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.total_hours), style = MaterialTheme.typography.labelLarge)
                    Text(formatDuration(state.totalMinutes), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.earnings), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            earningsLabel(state.earningsCents, state.revealed),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.toggleReveal(promptTitle) }) {
                            Icon(
                                if (state.revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(if (state.revealed) R.string.hide_earnings else R.string.show_earnings),
                            )
                        }
                    }
                }
            }
            PaidRow(
                state = state,
                onToggle = { if (state.paidStatus == PaidStatus.PAID) showUnpaidConfirm = true else showPaidPicker = true },
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.lessons, key = { it.id }) { lesson ->
                    LessonRow(
                        lesson,
                        onClick = { onEdit(lesson.id) },
                        showNotPaid = state.paidStatus == PaidStatus.PARTLY_PAID && lesson.paidOn == null,
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showPaidPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.today.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { showPaidPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.markPaid(pickerMillisToLocalDate(it)) }
                    showPaidPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showPaidPicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(
                state = pickerState,
                title = { Text(stringResource(R.string.paid_on_title), Modifier.padding(start = 24.dp, top = 16.dp)) },
            )
        }
    }

    if (showUnpaidConfirm) {
        AlertDialog(
            onDismissRequest = { showUnpaidConfirm = false },
            title = { Text(stringResource(R.string.mark_unpaid_question, monthTitle(month))) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markUnpaid()
                    showUnpaidConfirm = false
                }) { Text(stringResource(R.string.mark_unpaid)) }
            },
            dismissButton = { TextButton(onClick = { showUnpaidConfirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** "Paid by the school" tick: checked only when every lesson is paid; partly paid months say so underneath. */
@Composable
private fun PaidRow(state: MonthDetailUiState, onToggle: () -> Unit) {
    if (state.lessons.isEmpty()) return
    AlpineTile(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = state.paidStatus == PaidStatus.PAID, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.paid_by_school), style = MaterialTheme.typography.titleMedium)
                val date = state.paidOn?.let { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(it) }
                when {
                    state.paidStatus == PaidStatus.PARTLY_PAID -> Text(
                        stringResource(R.string.partly_paid) + (date?.let { " · " + stringResource(R.string.paid_on, it) } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PartlyPaidColor,
                    )
                    date != null -> Text(
                        stringResource(R.string.paid_on, date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}
