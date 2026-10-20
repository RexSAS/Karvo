package com.masterski.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterski.domain.LessonError
import com.masterski.domain.pickerMillisToLocalDate
import com.masterski.domain.toPickerMillis
import java.time.format.DateTimeFormatter
import java.util.Locale

private val QUICK_PICKS = listOf(60 to "1h", 90 to "1h 30m", 120 to "2h", 180 to "3h")

private fun LessonError.message() = when (this) {
    LessonError.StudentBlank -> "Enter a student name"
    LessonError.DurationZero -> "Duration must be more than 0"
    LessonError.DurationTooLong -> "Max 24 hours"
    LessonError.MinutesOver59 -> "Minutes must be 0–59"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLessonScreen(lessonId: Long?, onDone: () -> Unit) {
    val vm: EditLessonViewModel = viewModel(factory = EditLessonViewModel.factory(lessonId))
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var studentMenuOpen by remember { mutableStateOf(false) }

    val durationErrors = if (state.durationTouched) {
        state.errors.filter { it != LessonError.StudentBlank }
    } else {
        emptyList()
    }
    val studentError = LessonError.StudentBlank in state.errors && state.studentTouched

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New lesson" else "Edit lesson") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete lesson")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box {
                OutlinedTextField(
                    value = state.date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Transparent overlay so a tap anywhere on the field opens the picker.
                Box(Modifier.matchParentSize().clickable { showDatePicker = true })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.hoursText,
                    onValueChange = vm::onHoursChange,
                    label = { Text("Hours") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = durationErrors.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.minutesText,
                    onValueChange = vm::onMinutesChange,
                    label = { Text("Minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = durationErrors.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
            }
            if (durationErrors.isNotEmpty()) {
                Text(
                    durationErrors.joinToString("\n") { it.message() },
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val current = (state.hoursText.toIntOrNull() ?: 0) * 60 + (state.minutesText.toIntOrNull() ?: 0)
                QUICK_PICKS.forEach { (minutes, label) ->
                    FilterChip(
                        selected = state.durationTouched && current == minutes,
                        onClick = { vm.onQuickPick(minutes) },
                        label = { Text(label) },
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = studentMenuOpen && state.suggestions.isNotEmpty(),
                onExpandedChange = { studentMenuOpen = it },
            ) {
                OutlinedTextField(
                    value = state.student,
                    onValueChange = {
                        vm.onStudentChange(it)
                        studentMenuOpen = true
                    },
                    label = { Text("Student") },
                    singleLine = true,
                    isError = studentError,
                    supportingText = if (studentError) {
                        { Text(LessonError.StudentBlank.message()) }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = studentMenuOpen && state.suggestions.isNotEmpty(),
                    onDismissRequest = { studentMenuOpen = false },
                ) {
                    state.suggestions.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                vm.onStudentChange(name)
                                studentMenuOpen = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = vm::onNotesChange,
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { vm.save(onDone) },
                enabled = state.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.date.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { vm.onDateChange(pickerMillisToLocalDate(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this lesson?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.delete(onDone)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
}
