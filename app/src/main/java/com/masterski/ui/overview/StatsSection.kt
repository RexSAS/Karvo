package com.masterski.ui.overview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.masterski.R
import com.masterski.domain.OverviewStats
import com.masterski.domain.earningsLabel
import com.masterski.domain.formatDuration
import com.masterski.domain.shortMonthLabel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val BarCorner = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
private val StubHeight = 2.dp

/** Statistics shown below the month list. Money respects [revealed]; chart descriptions never contain money. */
@Composable
fun StatsSection(stats: OverviewStats, revealed: Boolean, rateSet: Boolean, modifier: Modifier = Modifier) {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.statistics), style = MaterialTheme.typography.titleLarge)
        SummaryCards(stats)
        LastSixMonthsCard(stats, revealed, locale)
        TopStudentsCard(stats, revealed, rateSet)
        BusiestDaysCard(stats, locale)
    }
}

@Composable
private fun SummaryCards(stats: OverviewStats) {
    // Equal-height cards even when one label wraps.
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(stats.lessonsThisMonth.toString(), stringResource(R.string.stats_lessons_this_month), Modifier.weight(1f))
        SummaryCard(formatDuration(stats.averageLessonMinutes), stringResource(R.string.stats_avg_lesson), Modifier.weight(1f))
        SummaryCard(stats.studentCount.toString(), stringResource(R.string.stats_students), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier) {
    ElevatedCard(modifier.fillMaxHeight()) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatsCard(title: String, description: String, content: @Composable () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun LastSixMonthsCard(stats: OverviewStats, revealed: Boolean, locale: Locale) {
    val bars = stats.lastSixMonths
    val max = bars.maxOf { it.minutes }.coerceAtLeast(1)
    val description = stringResource(
        R.string.a11y_last_six_months,
        bars.joinToString { "${shortMonthLabel(it.month, locale)} ${formatDuration(it.minutes)}" },
    )
    StatsCard(stringResource(R.string.stats_last_six_months), description) {
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bars.forEachIndexed { i, bar ->
                VerticalBar(
                    fraction = bar.minutes.toFloat() / max,
                    color = if (i == bars.lastIndex) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    // Leaves room for the value above and the month below within the row height.
                    maxHeight = 100.dp,
                    topLabel = formatDuration(bar.minutes),
                    bottomLabel = shortMonthLabel(bar.month, locale),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.stats_six_month_earnings),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                earningsLabel(stats.sixMonthEarningsCents, revealed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun TopStudentsCard(stats: OverviewStats, revealed: Boolean, rateSet: Boolean) {
    val top = stats.topStudents
    val max = top.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    val description = stringResource(R.string.a11y_top_students, top.joinToString { "${it.name} ${formatDuration(it.minutes)}" })
    StatsCard(stringResource(R.string.stats_top_students), description) {
        top.forEach { student ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        student.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.35f),
                    )
                    HorizontalBar(student.minutes.toFloat() / max, Modifier.weight(0.4f))
                    Text(
                        formatDuration(student.minutes),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.weight(0.25f),
                    )
                }
                if (rateSet) {
                    Text(
                        earningsLabel(student.earningsCents, revealed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BusiestDaysCard(stats: OverviewStats, locale: Locale) {
    val counts = stats.weekdayCounts
    val max = counts.max()
    val names = DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, locale) }
    val description = stringResource(
        R.string.a11y_busiest_days,
        DayOfWeek.entries.zip(counts).joinToString { (day, n) -> "${day.getDisplayName(TextStyle.FULL, locale)} $n" },
    )
    StatsCard(stringResource(R.string.stats_busiest_days), description) {
        Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            counts.forEachIndexed { i, n ->
                VerticalBar(
                    fraction = n.toFloat() / max.coerceAtLeast(1),
                    color = if (max > 0 && n == max) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    maxHeight = 60.dp,
                    topLabel = null,
                    bottomLabel = names[i],
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Animates from 0 to [target] the first time it is shown. */
@Composable
private fun animatedFraction(target: Float): Float {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val value by animateFloatAsState(if (started) target else 0f, tween(600), label = "bar")
    return value
}

@Composable
private fun VerticalBar(
    fraction: Float,
    color: Color,
    maxHeight: Dp,
    topLabel: String?,
    bottomLabel: String,
    modifier: Modifier = Modifier,
) {
    val f = animatedFraction(fraction)
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        if (topLabel != null) {
            Text(topLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            Spacer(Modifier.height(4.dp))
        }
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .height((maxHeight * f).coerceAtLeast(StubHeight))
                .background(color, BarCorner),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            bottomLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun HorizontalBar(fraction: Float, modifier: Modifier = Modifier) {
    val f = animatedFraction(fraction)
    val shape = RoundedCornerShape(5.dp)
    Box(modifier.height(10.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)) {
        Box(Modifier.fillMaxWidth(f.coerceAtLeast(0.02f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary, shape))
    }
}
