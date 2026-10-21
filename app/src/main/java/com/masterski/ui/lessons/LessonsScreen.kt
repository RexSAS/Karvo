package com.masterski.ui.lessons

import androidx.compose.ui.res.stringResource
import com.masterski.R
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterski.data.Lesson
import com.masterski.domain.monthTitle
import com.masterski.ui.components.LessonRow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LessonsScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    /** Called after a swipe-delete; the caller shows the Undo snackbar (hoisted so it outlives this screen). */
    onDeleted: (Lesson) -> Unit,
    viewModel: LessonsViewModel = viewModel(factory = LessonsViewModel.Factory),
) {
    val groupsOrNull by viewModel.groups.collectAsStateWithLifecycle()

    fun delete(lesson: Lesson) {
        viewModel.delete(lesson)
        onDeleted(lesson)
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.lessons)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_lesson)) }
        },
    ) { padding ->
        val groups = groupsOrNull
        if (groups == null) {
            // Still loading: show nothing rather than flashing the empty state.
        } else if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_lessons_hint), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                groups.forEach { group ->
                    stickyHeader(key = "header-${group.month}") {
                        Text(
                            monthTitle(group.month),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(group.lessons, key = { it.id }) { lesson ->
                        SwipeToDeleteRow(lesson, onDelete = { delete(lesson) }, onClick = { onEdit(lesson.id) })
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
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onError)
            }
        },
    ) {
        LessonRow(lesson, onClick = onClick)
    }
}
