package com.karvo.ui.lessons

import androidx.compose.foundation.layout.WindowInsets
import com.karvo.ui.theme.underBars
import com.karvo.ui.theme.LocalTabBarSpace
import androidx.compose.ui.res.stringResource
import com.karvo.R
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import com.karvo.onboarding.TourTarget
import com.karvo.onboarding.tourTarget
import com.karvo.ui.moments.rememberZone
import com.karvo.ui.moments.rememberMoments
import com.karvo.domain.momentCountsByMonth
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.semantics
import com.karvo.ui.theme.isAlpineNight
import com.karvo.ui.theme.rowNumber
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.res.pluralStringResource
import com.karvo.domain.formatDuration
import com.karvo.ui.theme.AddButton
import com.karvo.ui.theme.ScreenHeader
import com.karvo.ui.theme.groupedShape
import com.karvo.ui.theme.tileColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karvo.data.Lesson
import com.karvo.domain.monthTitle
import com.karvo.ui.components.EmptyState
import com.karvo.ui.components.LessonRow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LessonsScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    /** Called after a swipe-delete; the caller shows the Undo snackbar (hoisted so it outlives this screen). */
    onDeleted: (Lesson) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenMoments: () -> Unit,
    onOpenDay: (java.time.LocalDate) -> Unit,
    viewModel: LessonsViewModel = viewModel(factory = LessonsViewModel.Factory),
) {
    val groupsOrNull by viewModel.groups.collectAsStateWithLifecycle()
    val upcoming by viewModel.upcoming.collectAsStateWithLifecycle()
    val notesCard: @Composable (Modifier) -> Unit = { m -> com.karvo.ui.events.UpcomingEventsCard(upcoming, onOpenDay, m) }
    val moments = rememberMoments()
    val zone = rememberZone()
    val momentCounts = remember(moments, zone) { momentCountsByMonth(moments.orEmpty(), zone) }

    fun delete(lesson: Lesson) {
        viewModel.delete(lesson)
        onDeleted(lesson)
    }

    val tabBar = LocalTabBarSpace.current
    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        contentWindowInsets = WindowInsets(0), // the header pads itself; the list runs under the tab bar
        topBar = {
            val total = groupsOrNull.orEmpty().sumOf { it.lessons.size }
            ScreenHeader(
                title = stringResource(R.string.lessons),
                subtitle = if (total > 0) pluralStringResource(R.plurals.lesson_count, total, total) else null,
                trailing = {
                    IconButton(onClick = onOpenMoments, modifier = Modifier.tourTarget(TourTarget.Calendar)) { Icon(Icons.Filled.CalendarMonth, stringResource(R.string.calendar)) }
                },
            )
        },
        floatingActionButton = {
            Column(Modifier.padding(bottom = tabBar), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = onOpenCamera, modifier = Modifier.tourTarget(TourTarget.Camera)) { Icon(Icons.Filled.PhotoCamera, stringResource(R.string.take_moment)) }
                Box(Modifier.tourTarget(TourTarget.AddLesson)) { AddButton(onClick = onAdd, contentDescription = stringResource(R.string.add_lesson)) }
            }
        },
    ) { padding ->
        val groups = groupsOrNull
        if (groups == null) {
            // Still loading: show nothing rather than flashing the empty state.
        } else if (groups.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(bottom = tabBar)) {
              notesCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
              EmptyState(stringResource(R.string.no_lessons_hint), Modifier.fillMaxSize().padding(32.dp))
            }
        } else {
            // Under the header and the tab bar; the end stays clear of the add buttons.
            LazyColumn(Modifier.fillMaxSize(), contentPadding = underBars(padding, extra = 96.dp, bottomBar = tabBar)) {
                item(key = "notes") { notesCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                groups.forEach { group ->
                    item(key = "header-${group.month}") {
                        // The month on the left; its moments (if any) and its hours on the right. Read as one line.
                        Row(
                            Modifier.padding(start = 20.dp, end = 24.dp, top = 20.dp, bottom = 8.dp).semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(monthTitle(group.month), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            momentCounts[group.month]?.let { count ->
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = stringResource(R.string.moments),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp))
                            }
                            Text(
                                formatDuration(group.totalMinutes),
                                style = MaterialTheme.typography.rowNumber,
                                // Glacier blue in Alpine Night; Classic's primary can be too dim on a dark page.
                                color = if (isAlpineNight()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    itemsIndexed(group.lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                        SwipeToDeleteRow(
                            lesson,
                            shape = groupedShape(index, group.lessons.size),
                            onDelete = { delete(lesson) },
                            onClick = { onEdit(lesson.id) },
                        )
                        if (index < group.lessons.lastIndex) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 32.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(lesson: Lesson, shape: Shape, onDelete: () -> Unit, onClick: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    // Never settle in the dismissed state: the row springs back and the deletion waits for confirmation.
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) confirmDelete = true
            false
        },
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_lesson_question)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    SwipeToDismissBox(
        state = state,
        modifier = Modifier.padding(horizontal = 16.dp).clip(shape),
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
        LessonRow(lesson, onClick = onClick, containerColor = tileColor())
    }
}
