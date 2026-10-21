package com.karvo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karvo.R

/** The dialogs of the import flow; [ImportState.Done] is shown by the screen as a snackbar. */
@Composable
fun ImportDialogs(state: ImportState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    when (state) {
        ImportState.Idle, is ImportState.Done -> Unit
        ImportState.Reading, ImportState.Importing -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(R.string.import_csv)) },
            text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
        )
        is ImportState.Preview -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.import_preview_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val add = pluralStringResource(R.plurals.import_preview_add, state.plan.toAdd.size, state.plan.toAdd.size)
                    val skipped = state.plan.skipped
                    Text(if (skipped == 0) add else add + " · " + pluralStringResource(R.plurals.import_preview_skipped, skipped, skipped))
                    Text(
                        stringResource(R.string.import_preview_backup),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.import_button)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        )
        else -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.import_csv)) },
            text = {
                Text(
                    when (state) {
                        is ImportState.Invalid -> stringResource(R.string.import_invalid, state.line)
                        ImportState.Empty -> stringResource(R.string.import_empty)
                        is ImportState.NothingNew -> pluralStringResource(R.plurals.import_nothing_new, state.total, state.total)
                        ImportState.BackupFailed -> stringResource(R.string.restore_backup_error)
                        else -> stringResource(R.string.import_failed)
                    },
                )
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        )
    }
}
