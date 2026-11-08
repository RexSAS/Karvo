package com.karvo.ui.events

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.karvo.R
import com.karvo.data.Event
import com.karvo.data.MAX_EVENT
import com.karvo.notes.canRingExactly
import com.karvo.ui.components.accentGold
import java.time.LocalDate

@Composable
private fun eventCardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

/** Home: the events of today and the next 3 days ("Today", "Tomorrow", "In 2 days"); tapping one opens that day. */
@Composable
fun UpcomingEventsCard(days: List<Triple<LocalDate, Int, List<Event>>>, onOpenDay: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    if (days.isEmpty()) return
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = eventCardColors()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(stringResource(R.string.events), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            days.forEach { (date, inDays, events) ->
                events.forEachIndexed { i, e ->
                    Row(Modifier.fillMaxWidth().clickable { onOpenDay(date) }.padding(vertical = 6.dp)) {
                        Text(if (i > 0) "" else when (inDays) {
                            0 -> stringResource(R.string.today)
                            1 -> stringResource(R.string.tomorrow)
                            else -> pluralStringResource(R.plurals.in_days, inDays, inDays)
                        }, color = accentGold(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(96.dp))
                        Text(e.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            ExactReminderButton()
        }
    }
}

/** Day page: that day's events, with add / edit / delete. */
@Composable
fun DayEventsSection(events: List<Event>, onAdd: (String) -> Unit, onUpdate: (Event) -> Unit, onDelete: (Event) -> Unit, modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf<Event?>(null) }
    var adding by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askOnce() {
        // The 07:30 reminder needs notifications on Android 13+: ask when an event is added.
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = eventCardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Event, contentDescription = null, tint = accentGold())
                Text(stringResource(R.string.events), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp).weight(1f))
                OutlinedButton(onClick = { adding = true }) { Text(stringResource(R.string.add_event)) }
            }
            if (events.isEmpty()) Text(stringResource(R.string.no_events_this_day), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            events.forEach { e ->
                Text(e.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().clickable { editing = e }.padding(vertical = 6.dp))
            }
            if (events.isNotEmpty()) ExactReminderButton()
        }
    }
    if (adding) EventDialog(null, onDismiss = { adding = false }, onSave = { onAdd(it); adding = false; askOnce() }, onDelete = {})
    editing?.let { e -> EventDialog(e, onDismiss = { editing = null }, onSave = { onUpdate(e.copy(text = it)); editing = null }, onDelete = { onDelete(e); editing = null }) }
}

/** Without "Alarms & reminders" Android may deliver the 07:30 reminder up to an hour late: offer to allow it. */
@Composable
private fun ExactReminderButton() {
    val context = LocalContext.current
    var exact by remember { mutableStateOf(canRingExactly(context)) }
    LifecycleResumeEffect(Unit) { exact = canRingExactly(context); onPauseOrDispose { } }
    if (exact) return
    TextButton(onClick = {
        // Some phones lack this settings screen: the app's own settings page is the fallback.
        runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))) }
            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))) } }
    }) { Text(stringResource(R.string.event_exact)) }
}

@Composable
private fun EventDialog(event: Event?, onDismiss: () -> Unit, onSave: (String) -> Unit, onDelete: () -> Unit) {
    var text by remember { mutableStateOf(event?.text ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (event == null) R.string.new_event else R.string.edit_event)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(text, { if (it.length <= MAX_EVENT) text = it }, label = { Text(stringResource(R.string.event)) },
                    placeholder = { Text(stringResource(R.string.event_placeholder)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.event_reminder_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                if (event != null) TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_event_question)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
