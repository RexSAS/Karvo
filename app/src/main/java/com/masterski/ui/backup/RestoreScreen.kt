package com.masterski.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterski.R

/** Two "accept the risk" pages, then "back up first?", then the restore. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    fileName: String,
    onBack: () -> Unit,
    onRestored: (count: Int) -> Unit,
    viewModel: RestoreViewModel = viewModel(key = "restore-$fileName", factory = RestoreViewModel.factory(fileName)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val result = state.result
    LaunchedEffect(result) { if (result is RestoreResult.Done) onRestored(result.count) }
    val goBack = if (state.step == 2) viewModel::back else onBack

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(if (state.step == 1) R.string.restore_title else R.string.restore_last_check_title)) },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = goBack) {
                    Text(stringResource(if (state.step == 1) R.string.cancel else R.string.back))
                }
                if (state.step == 1) {
                    Button(onClick = viewModel::next, enabled = state.understood && state.backup != null) {
                        Text(stringResource(R.string.continue_))
                    }
                } else {
                    Button(
                        onClick = viewModel::requestRestore,
                        enabled = state.acceptedRisk && !state.busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text(stringResource(R.string.restore)) }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            val backup = state.backup
            if (state.step == 1) {
                if (backup != null) {
                    Text(
                        stringResource(R.string.restore_from, backupDateText(backup, state.zoneId), backupReasonText(backup.reason)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.restore_replace_warning, state.currentCount, backup.lessonCount),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                CheckRow(stringResource(R.string.restore_understood), state.understood, viewModel::setUnderstood)
            } else {
                Text(stringResource(R.string.restore_last_check_text), style = MaterialTheme.typography.bodyLarge)
                CheckRow(stringResource(R.string.restore_accept_risk), state.acceptedRisk, viewModel::setAcceptedRisk)
            }
        }
        if (state.busy) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }

    if (state.askBackupFirst) {
        val oldest = state.oldestToRemove
        AlertDialog(
            onDismissRequest = viewModel::dismissAsk,
            title = { Text(stringResource(R.string.restore_ask_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.restore_ask_text, state.currentCount))
                    if (oldest != null) {
                        Text(
                            stringResource(R.string.restore_ask_oldest, backupDateText(oldest, state.zoneId)),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.restore(backupFirst = true) }) { Text(stringResource(R.string.restore_backup_and_restore)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.restore(backupFirst = false) }) { Text(stringResource(R.string.restore_without_backup)) }
            },
        )
    }

    if (result is RestoreResult.ReadError || result is RestoreResult.BackupError) {
        AlertDialog(
            onDismissRequest = onBack,
            text = {
                Text(
                    when (result) {
                        is RestoreResult.ReadError -> context.getString(R.string.restore_read_error, result.line)
                        else -> context.getString(R.string.restore_backup_error)
                    },
                )
            },
            confirmButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onChange, role = Role.Checkbox)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}
