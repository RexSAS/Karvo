package com.masterski.ui.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterski.data.Lesson
import com.masterski.ui.components.LessonRow
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LessonsScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: LessonsViewModel = viewModel(factory = LessonsViewModel.Factory),
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

    fun onDeleted(lesson: Lesson) {
        viewModel.delete(lesson)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                "Lesson deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(lesson)
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Lessons") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Add lesson") }
        },
    ) { padding ->
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No lessons yet. Tap + to add your first one.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                groups.forEach { group ->
                    stickyHeader(key = "header-${group.month}") {
                        Text(
                            group.month.format(monthFormatter),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(group.lessons, key = { it.id }) { lesson ->
                        SwipeToDeleteRow(lesson, onDelete = { onDeleted(lesson) }, onClick = { onEdit(lesson.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(lesson: Lesson, onDelete: () -> Unit, onClick: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onError)
            }
        },
    ) {
        LessonRow(lesson, onClick = onClick)
    }
}
