package com.karvo.ui.overview

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import com.karvo.ui.theme.groupedShape
import com.karvo.ui.theme.tileColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karvo.R
import com.karvo.domain.StudentDetail
import com.karvo.domain.chartDurationLabel
import com.karvo.domain.earningsLabel
import com.karvo.domain.formatDuration
import com.karvo.domain.monthTitle
import com.karvo.domain.shortMonthLabel
import com.karvo.ui.components.LessonRow
import com.karvo.ui.theme.AlpineTile
import com.karvo.ui.theme.headerFade
import com.karvo.ui.theme.isAlpineNight
import com.karvo.ui.theme.underBars
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Everything about one student: totals, the last 6 months, this season against the last, and all their lessons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreen(
    studentKey: String,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: StudentViewModel = viewModel(key = "student-$studentKey", factory = StudentViewModel.factory(studentKey)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Hidden each time the screen opens and whenever it stops; showing may ask for fingerprint/PIN.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.hide() }
    val promptTitle = stringResource(R.string.show_earnings)
    val detail = state.detail
    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.headerFade(),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { Text(detail?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        if (detail == null) {
            // Their last lesson was deleted while the page was open.
            Text(stringResource(R.string.student_no_lessons), Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = underBars(padding, extra = 16.dp)) {
            item(key = "totals") {
                Totals(detail, state.rateSet, state.revealed) { viewModel.toggleReveal(promptTitle) }
            }
            item(key = "months") { LastSixMonths(detail) }
            item(key = "seasons") { Seasons(detail) }
            detail.months.forEach { month ->
                item(key = "header-${month.month}") {
                    Row(Modifier.padding(start = 20.dp, end = 24.dp, top = 20.dp, bottom = 8.dp).semantics(mergeDescendants = true) {}) {
                        Text(monthTitle(month.month), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text(
                            formatDuration(month.minutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isAlpineNight()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Grouped in a rounded card per month, as on the Lessons page.
                itemsIndexed(month.lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                    LessonRow(
                        lesson,
                        onClick = { onEdit(lesson.id) },
                        modifier = Modifier.padding(horizontal = 16.dp).clip(groupedShape(index, month.lessons.size)),
                        containerColor = tileColor(),
                    )
                    if (index < month.lessons.lastIndex) {
                        HorizontalDivider(Modifier.padding(horizontal = 32.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Totals(detail: StudentDetail, rateSet: Boolean, revealed: Boolean, onToggleReveal: () -> Unit) {
    val date = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    AlpineTile(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(stringResource(R.string.total_hours), style = MaterialTheme.typography.labelLarge)
                Text(formatDuration(detail.totalMinutes), style = MaterialTheme.typography.displaySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Figure(detail.lessonCount.toString(), stringResource(R.string.lessons))
                Figure(formatDuration(detail.averageMinutes), stringResource(R.string.stats_avg_lesson))
            }
            Text(
                stringResource(R.string.student_first_last, detail.firstLesson.format(date), detail.lastLesson.format(date)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (rateSet) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.earnings), style = MaterialTheme.typography.labelLarge)
                        Text(earningsLabel(detail.earningsCents, revealed), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    IconButton(onClick = onToggleReveal) {
                        Icon(
                            if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(if (revealed) R.string.hide_earnings else R.string.show_earnings),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Figure(value: String, label: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LastSixMonths(detail: StudentDetail) {
    val locale = Locale.getDefault()
    val bars = detail.lastSixMonths
    val max = bars.maxOf { it.minutes }.coerceAtLeast(1)
    val description = stringResource(
        R.string.a11y_last_six_months,
        bars.joinToString { "${shortMonthLabel(it.month, locale)} ${formatDuration(it.minutes)}" },
    )
    Column(Modifier.padding(horizontal = 16.dp)) {
        StatsCard(stringResource(R.string.stats_last_six_months), description) {
            RidgeChart(
                fractions = bars.map { it.minutes.toFloat() / max },
                topLabels = bars.map { chartDurationLabel(it.minutes) },
                bottomLabels = bars.map { shortMonthLabel(it.month, locale) },
                modifier = Modifier.fillMaxWidth().height(150.dp),
            )
        }
    }
}

@Composable
private fun Seasons(detail: StudentDetail) {
    AlpineTile(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Figure(formatDuration(detail.thisSeasonMinutes), stringResource(R.string.this_season, detail.season.label()))
            Figure(formatDuration(detail.lastSeasonMinutes), stringResource(R.string.student_last_season))
        }
    }
}

private fun LocalDate?.format(formatter: DateTimeFormatter) = this?.let(formatter::format).orEmpty()
