package com.karvo.ui.settings

import androidx.compose.ui.res.stringResource
import com.karvo.R
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.karvo.data.LanguageChoice
import com.karvo.data.ThemeChoice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karvo.BuildConfig
import com.karvo.ui.theme.ScreenHeader
import com.karvo.ui.theme.alpineFieldColors
import com.karvo.domain.csvFileBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenBackups: () -> Unit,
    onCheckForUpdates: () -> Unit,
    /** False while another pager page is on screen: a typed rate is saved as soon as the user swipes away. */
    isCurrentPage: Boolean = true,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    importViewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importState by importViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showZonePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var rateFocused by remember { mutableStateOf(false) }

    // Besides Done and focus loss, save a typed rate when the app stops or the screen leaves
    // (saveRate is idempotent; the ViewModel outlives this composition, so the write completes).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveRate() }
    DisposableEffect(viewModel) { onDispose { viewModel.saveRate() } }
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            viewModel.saveRate()
            focusManager.clearFocus()
        }
    }

    suspend fun showMessage(message: String) {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
    }

    fun failureMessage(e: Exception) = context.getString(R.string.export_failed, e.message ?: e.javaClass.simpleName)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = try {
                val export = viewModel.exportData()
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri)
                        ?: throw java.io.IOException(context.getString(R.string.cannot_open_file))
                    out.use { it.write(csvFileBytes(export.csv)) }
                }
                context.resources.getQuantityString(R.plurals.exported_lessons, export.lessonCount, export.lessonCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failureMessage(e)
            }
            showMessage(message)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importViewModel.onFilePicked(uri.toString())
    }

    ImportDialogs(importState, importViewModel::confirm, importViewModel::dismiss)
    val doneCount = (importState as? ImportState.Done)?.count
    LaunchedEffect(doneCount) {
        if (doneCount != null) {
            importViewModel.dismiss()
            showMessage(context.resources.getQuantityString(R.plurals.imported_lessons, doneCount, doneCount))
        }
    }

    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = { ScreenHeader(title = stringResource(R.string.settings)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                colors = alpineFieldColors(),
                value = state.rateText,
                onValueChange = viewModel::onRateChange,
                label = { Text(stringResource(R.string.hourly_rate)) },
                placeholder = { Text("00.00") },
                suffix = { Text("€") },
                singleLine = true,
                isError = state.rateError,
                supportingText = {
                    Text(stringResource(if (state.rateError) R.string.rate_error else R.string.rate_hint))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    viewModel.saveRate()
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (rateFocused && !it.isFocused) viewModel.saveRate()
                        rateFocused = it.isFocused
                    },
            )

            ListItem(
                colors = transparentRow(),
                headlineContent = { Text(stringResource(R.string.time_zone)) },
                supportingContent = { Text(state.timeZoneId) },
                modifier = Modifier.clickable { showZonePicker = true },
            )

            ListItem(
                colors = transparentRow(),
                headlineContent = { Text(stringResource(R.string.theme)) },
                supportingContent = { Text(themeLabel(state.theme)) },
                modifier = Modifier.clickable { showThemePicker = true },
            )

            ListItem(
                colors = transparentRow(),
                headlineContent = { Text(stringResource(R.string.language)) },
                supportingContent = { Text(languageLabel(state.language)) },
                modifier = Modifier.clickable { showLanguagePicker = true },
            )

            ListItem(
                colors = transparentRow(),
                leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.check_for_updates)) },
                supportingContent = { Text(stringResource(R.string.version, BuildConfig.VERSION_NAME)) },
                modifier = Modifier.clickable(onClick = onCheckForUpdates),
            )

            ListItem(
                colors = transparentRow(),
                leadingContent = { Icon(Icons.Filled.Restore, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.backups)) },
                supportingContent = { Text(stringResource(R.string.backups_hint)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenBackups),
            )

            // A navigation-style row, not a filled button, so it doesn't read as "Save".
            ListItem(
                colors = transparentRow(),
                leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.export_csv)) },
                supportingContent = { Text(stringResource(R.string.export_csv_hint)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val fileName = try {
                            viewModel.exportFileName()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            showMessage(failureMessage(e))
                            return@launch
                        }
                        exportLauncher.launch(fileName)
                    }
                },
            )

            ListItem(
                colors = transparentRow(),
                leadingContent = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.import_csv)) },
                supportingContent = { Text(stringResource(R.string.import_csv_hint)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable {
                    // Some file managers label CSV files as plain text or as a generic binary type.
                    importLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "application/octet-stream"))
                },
            )

            Text(
                stringResource(R.string.version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showZonePicker) {
        TimeZoneDialog(
            current = state.timeZoneId,
            onSelect = {
                viewModel.onTimeZoneSelected(it)
                showZonePicker = false
            },
            onDismiss = { showZonePicker = false },
        )
    }

    if (showThemePicker) {
        ChoiceDialog(
            title = stringResource(R.string.theme),
            options = ThemeChoice.entries,
            selected = state.theme,
            label = { themeLabel(it) },
            leading = { ThemeSwatch(it) },
            onSelect = {
                viewModel.onThemeSelected(it)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false },
        )
    }

    if (showLanguagePicker) {
        ChoiceDialog(
            title = stringResource(R.string.language),
            options = LanguageChoice.entries,
            selected = state.language,
            label = { languageLabel(it) },
            onSelect = {
                showLanguagePicker = false
                viewModel.onLanguageSelected(it)
            },
            onDismiss = { showLanguagePicker = false },
        )
    }
}

@Composable
private fun themeLabel(choice: ThemeChoice): String = stringResource(
    when (choice) {
        ThemeChoice.ALPINE_NIGHT -> R.string.theme_alpine_night
        ThemeChoice.CLASSIC -> R.string.theme_classic
    },
)

/** Language names are shown in their own language, whatever the current UI language. */
@Composable
private fun languageLabel(choice: LanguageChoice): String = when (choice) {
    LanguageChoice.SYSTEM -> stringResource(R.string.language_system)
    LanguageChoice.ITALIAN -> "Italiano"
    LanguageChoice.ENGLISH -> "English"
}

@Composable
private fun ThemeSwatch(choice: ThemeChoice) {
    val (fill, border) = when (choice) {
        ThemeChoice.ALPINE_NIGHT -> Color(0xFF0B1220) to Color(0xFF7CC4FA)
        ThemeChoice.CLASSIC -> Color(0xFF1565C0) to Color(0xFF1565C0)
    }
    Box(Modifier.size(20.dp).clip(CircleShape).background(fill).border(2.dp, border, CircleShape))
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    leading: (@Composable (T) -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, onClick = { onSelect(option) }, role = Role.RadioButton)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        leading?.invoke(option)
                        Text(label(option))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeZoneDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) availableTimeZones else availableTimeZones.filter { it.contains(q, ignoreCase = true) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.time_zone)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered, key = { it }) { id ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    id,
                                    color = if (id == current) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            modifier = Modifier.clickable { onSelect(id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** Settings rows let the alpine backdrop show through instead of cutting it into fragments. */
@Composable
private fun transparentRow() = ListItemDefaults.colors(containerColor = Color.Transparent)
