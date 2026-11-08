package com.karvo.ui.lessons

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.karvo.R
import com.karvo.data.MAX_DAY_NOTE
import com.karvo.domain.pickerMillisToLocalDate
import com.karvo.domain.toPickerMillis
import com.karvo.ui.components.accentGold
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM")

/** Today's and upcoming day notes, with "+ Note"; tapping a note edits it. */
@Composable
fun NotesCard(notes: List<Pair<LocalDate, String>>, today: LocalDate, onSave: (LocalDate, String?) -> Unit, modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf<Pair<LocalDate?, String>?>(null) }
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.day_notes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { editing = null to "" }) { Text(stringResource(R.string.add_note)) }
            }
            // Without "Alarms & reminders" Android may deliver the 07:30 reminder up to an hour late.
            var exact by remember { mutableStateOf(com.karvo.notes.canRingExactly(context)) }
            androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { exact = com.karvo.notes.canRingExactly(context); onPauseOrDispose { } }
            if (notes.isNotEmpty() && !exact) TextButton(onClick = {
                // Some phones lack this settings screen: the app's own settings page is the fallback.
                runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:" + context.packageName))) }
                    .onFailure { runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName))) } }
            }) { Text(stringResource(R.string.note_exact)) }
            if (notes.isEmpty()) Text(stringResource(R.string.no_notes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            notes.forEach { (date, text) ->
                Row(Modifier.fillMaxWidth().clickable { editing = date to text }.padding(vertical = 6.dp)) {
                    Text(if (date == today) stringResource(R.string.today) else date.format(dayFormat),
                        color = accentGold(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(96.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    editing?.let { (date, text) ->
        NoteDialog(date, text, today, onDismiss = { editing = null }, onSave = { d, t ->
            onSave(d, t)
            editing = null
            // The 07:30 reminder needs notifications on Android 13+: ask when the first note is saved.
            if (!t.isNullOrBlank() && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteDialog(date: LocalDate?, initial: String, today: LocalDate, onDismiss: () -> Unit, onSave: (LocalDate, String?) -> Unit) {
    var day by remember { mutableStateOf(date ?: today) }
    var text by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (date == null) R.string.new_note else R.string.edit_note)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { picking = true }, enabled = date == null) { Text(day.format(dayFormat)) }
                OutlinedTextField(text, { if (it.length <= MAX_DAY_NOTE) text = it }, label = { Text(stringResource(R.string.note)) },
                    placeholder = { Text(stringResource(R.string.note_placeholder)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.note_reminder_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(day, text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                if (date != null) TextButton(onClick = { onSave(day, null) }) { Text(stringResource(R.string.delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day.toPickerMillis())
        DatePickerDialog(onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { day = pickerMillisToLocalDate(it) }; picking = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = state) }
    }
}
