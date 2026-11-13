package com.karvo.ui.overview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.karvo.ui.theme.AlpineTile
import com.karvo.ui.theme.KarvoMotion
import com.karvo.ui.theme.TileShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.karvo.R
import com.karvo.domain.OverviewStats
import com.karvo.domain.chartDurationLabel
import com.karvo.domain.earningsLabel
import com.karvo.domain.formatDuration
import com.karvo.domain.shortMonthLabel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val BarCorner = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
private val StubHeight = 2.dp

/** Charts below the headline tiles. Money respects [revealed]; chart descriptions never contain money. */
@Composable
fun StatsSection(stats: OverviewStats, revealed: Boolean, rateSet: Boolean, onOpenStudent: (String) -> Unit, modifier: Modifier = Modifier) {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LastSixMonthsCard(stats, revealed, locale)
        TopStudentsCard(stats, revealed, rateSet, onOpenStudent)
        BusiestDaysCard(stats, locale)
    }
}

/** The three headline numbers; Overview places them overlapping the season total. */
@Composable
fun SummaryTiles(stats: OverviewStats, modifier: Modifier = Modifier, lessonsInScope: Int? = null) {
    // Equal-height cards even when one label wraps.
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // This season: the lessons of this month. A past season or Lifetime: all of its lessons ([lessonsInScope]).
        if (lessonsInScope == null) SummaryCard(stats.lessonsThisMonth.toString(), stringResource(R.string.stats_lessons_this_month), Modifier.weight(1f))
        else SummaryCard(lessonsInScope.toString(), stringResource(R.string.lessons), Modifier.weight(1f))
        SummaryCard(formatDuration(stats.averageLessonMinutes), stringResource(R.string.stats_avg_lesson), Modifier.weight(1f))
        SummaryCard(stats.studentCount.toString(), stringResource(R.string.stats_students), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier) {
    AlpineTile(modifier.fillMaxHeight(), shape = TileShape) {
        Column(Modifier.padding(12.dp)) {
            // Narrow phones: shrink until it fits on one line ("1h 42m" used to break and show only "1h").
            val base = MaterialTheme.typography.headlineMedium
            var style by remember(value, base) { mutableStateOf(base) }
            Text(value, style = style, maxLines = 1, softWrap = false, onTextLayout = {
                if (it.didOverflowWidth && style.fontSize > base.fontSize * 0.5f) style = style.copy(fontSize = style.fontSize * 0.9f)
            })
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun StatsCard(title: String, description: String?, content: @Composable () -> Unit) {
    // Read as one [description] (no money in it); a null description lets the content speak for itself.
    AlpineTile(Modifier.fillMaxWidth().then(if (description != null) Modifier.clearAndSetSemantics { contentDescription = description } else Modifier)) {
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
        RidgeChart(
            fractions = bars.map { it.minutes.toFloat() / max },
            topLabels = bars.map { chartDurationLabel(it.minutes) },
            bottomLabels = bars.map { shortMonthLabel(it.month, locale) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
        )
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
private fun TopStudentsCard(stats: OverviewStats, revealed: Boolean, rateSet: Boolean, onOpenStudent: (String) -> Unit) {
    // Each student is read (and opened) on their own, so no description for the whole card.
    StatsCard(stringResource(R.string.stats_top_students), description = null) {
        TopStudentsPodium(stats.topStudents, revealed, rateSet, onOpenStudent)
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
    val value by animateFloatAsState(if (started) target else 0f, tween(KarvoMotion.Long, easing = KarvoMotion.Ease), label = "bar")
    return value
}

/**
 * The months as a mountain ridge: one summit per month, as high as its hours, with the newest month marked in gold.
 * Labels sit in equal columns above and below, so each one lines up with its summit.
 */
@Composable
internal fun RidgeChart(fractions: List<Float>, topLabels: List<String>, bottomLabels: List<String>, modifier: Modifier = Modifier) {
    val grow = animatedFraction(1f)
    val line = MaterialTheme.colorScheme.primary
    val newest = MaterialTheme.colorScheme.tertiary
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            topLabels.forEach { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        // The ridge scales within whatever height the labels leave, so large fonts shrink it instead of clipping it.
        Canvas(Modifier.weight(1f).fillMaxWidth().padding(vertical = 6.dp)) {
            val column = size.width / fractions.size
            val top = 4.dp.toPx() // room for the summit dot
            val points = fractions.mapIndexed { i, f -> Offset(column * (i + .5f), top + (size.height - top) * (1 - f * grow)) }
            val ridge = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) } }
            val ground = Path().apply { addPath(ridge); lineTo(points.last().x, size.height); lineTo(points.first().x, size.height); close() }
            drawPath(ground, Brush.verticalGradient(listOf(line.copy(alpha = .34f), line.copy(alpha = 0f))))
            drawPath(ridge, line, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            points.dropLast(1).forEach { drawCircle(line, 2.5.dp.toPx(), it) }
            drawCircle(newest, 4.5.dp.toPx(), points.last())
        }
        Row(Modifier.fillMaxWidth()) {
            bottomLabels.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VerticalBar(
    fraction: Float,
    color: Color,
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
        // The bar scales within whatever height the labels leave, so large fonts shrink bars instead of clipping them.
        Box(Modifier.weight(1f).fillMaxWidth(0.7f), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(f).heightIn(min = StubHeight).background(color, BarCorner))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            bottomLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
