package com.masterski.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masterski.R
import java.util.Locale

/** Release notes are Markdown on GitHub; show them as plain text. */
private fun plainNotes(markdown: String): String =
    markdown.replace("**", "").replace("`", "").replace(Regex("(?m)^#+\\s*"), "").trim()

private fun megabytes(bytes: Long): String = String.format(Locale.getDefault(), "%.1f", bytes / 1_000_000.0)

/** Every dialog of the update flow; hosted once, above the navigation graph. */
@Composable
fun UpdateDialogs(vm: UpdateViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.checkOnStart() }
    // Back from the "Install unknown apps" page: continue or report (no-op unless we sent the user there).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onReturnedFromPermissionSettings() }

    when (val s = state) {
        UpdateState.Idle -> Unit
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text(stringResource(R.string.update_available_title, s.release.version.toString())) },
            text = {
                Text(
                    plainNotes(s.release.notes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = vm::update) { Text(stringResource(R.string.update)) } },
            dismissButton = { TextButton(onClick = vm::dismiss) { Text(stringResource(R.string.later)) } },
        )
        UpdateState.Checking -> BusyDialog(stringResource(R.string.checking_for_updates))
        UpdateState.BackingUp -> BusyDialog(stringResource(R.string.update_backing_up))
        UpdateState.Installing -> BusyDialog(stringResource(R.string.update_installing))
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.update_downloading)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { if (s.total > 0) (s.read.toFloat() / s.total).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.update_progress, megabytes(s.read), megabytes(s.total)), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = vm::cancelDownload) { Text(stringResource(R.string.cancel)) } },
        )
        UpdateState.NeedsPermission -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text(stringResource(R.string.update_permission_title)) },
            text = { Text(stringResource(R.string.update_permission_text)) },
            confirmButton = { TextButton(onClick = vm::openPermissionSettings) { Text(stringResource(R.string.open_settings)) } },
            dismissButton = { TextButton(onClick = vm::dismiss) { Text(stringResource(R.string.cancel)) } },
        )
        UpdateState.UpToDate -> MessageDialog(stringResource(R.string.up_to_date), vm::dismiss)
        UpdateState.CheckFailed -> MessageDialog(stringResource(R.string.check_failed), vm::dismiss)
        is UpdateState.Failed -> MessageDialog(
            if (s.code != null) stringResource(s.message, s.code) else stringResource(s.message),
            vm::dismiss,
        )
    }
}

@Composable
private fun BusyDialog(text: String) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(text)
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun MessageDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}
