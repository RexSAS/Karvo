package com.karvo.ui.edit

import androidx.compose.ui.res.stringResource
import com.karvo.R
import com.karvo.ui.messageRes
import com.karvo.ui.theme.alpineFieldColors
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karvo.domain.LessonError
import androidx.compose.material3.MaterialTheme
import com.karvo.ui.components.PartlyPaidColor
import com.karvo.domain.monthTitle
import com.karvo.domain.pickerMillisToLocalDate
import com.karvo.domain.toPickerMillis
import java.time.format.DateTimeFormatter
import java.util.Locale

private val QUICK_PICKS = listOf(60 to "1h", 90 to "1h 30m", 120 to "2h", 180 to "3h")

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
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(if (state.isNew) R.string.new_lesson else R.string.edit_lesson)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_lesson))
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
                    colors = alpineFieldColors(),
                    value = state.date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.date)) },
                    trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Transparent overlay so a tap anywhere on the field opens the picker.
                Box(Modifier.matchParentSize().clickable { showDatePicker = true })
            }
            state.paidMonthNote?.let { month ->
                Text(
                    stringResource(R.string.paid_month_note, monthTitle(month)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PartlyPaidColor,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    colors = alpineFieldColors(),
                    value = state.hoursText,
                    onValueChange = vm::onHoursChange,
                    label = { Text(stringResource(R.string.hours)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = durationErrors.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    colors = alpineFieldColors(),
                    value = state.minutesText,
                    onValueChange = vm::onMinutesChange,
                    label = { Text(stringResource(R.string.minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = durationErrors.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
            }
            if (durationErrors.isNotEmpty()) {
                val messages = durationErrors.map { stringResource(it.messageRes()) }
                Text(
                    messages.joinToString("\n"),
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
                    colors = alpineFieldColors(),
                    value = state.student,
                    onValueChange = {
                        vm.onStudentChange(it)
                        studentMenuOpen = true
                    },
                    label = { Text(stringResource(R.string.student)) },
                    singleLine = true,
                    isError = studentError,
                    supportingText = if (studentError) {
                        { Text(stringResource(LessonError.StudentBlank.messageRes())) }
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
                colors = alpineFieldColors(),
                value = state.notes,
                onValueChange = vm::onNotesChange,
                label = { Text(stringResource(R.string.notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { vm.save(onDone) },
                enabled = state.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
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
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_lesson_question)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.delete(onDone)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
