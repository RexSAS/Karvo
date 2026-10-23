package com.karvo.ui.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karvo.KarvoApp
import com.karvo.R
import com.karvo.domain.Moment
import com.karvo.domain.calendarCells
import com.karvo.domain.dayTitle
import com.karvo.domain.monthTitle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Every moment in the Karvo album; reloads on resume, e.g. after a capture or a deletion. */
@Composable
fun rememberMoments(): List<Moment>? {
    val container = (LocalContext.current.applicationContext as KarvoApp).container
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val moments by produceState<List<Moment>?>(null, tick) { value = container.momentStore.list() }
    return moments
}

@Composable
fun rememberZone(): ZoneId {
    val settings = (LocalContext.current.applicationContext as KarvoApp).container.settingsRepository
    val zone by produceState(ZoneId.systemDefault()) { value = ZoneId.of(settings.timeZoneId.first()) }
    return zone
}

/** The season as a calendar: each day shows its first moment; days with only lessons get a dot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(onBack: () -> Unit, onOpenCamera: () -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val container = (LocalContext.current.applicationContext as KarvoApp).container
    val moments = rememberMoments()
    val zone = rememberZone()
    val lessons by container.lessonRepository.observeLessons().collectAsStateWithLifecycle(initialValue = null)
    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.moments)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = onOpenCamera) { Icon(Icons.Filled.PhotoCamera, stringResource(R.string.take_moment)) }
                },
            )
        },
    ) { padding ->
        val allMoments = moments
        val allLessons = lessons
        if (allMoments == null || allLessons == null) return@Scaffold // loading
        val byDay = remember(allMoments, zone) {
            allMoments.groupBy { it.takenAt.atZone(zone).toLocalDate() }.mapValues { (_, items) -> items.sortedBy { it.takenAt } }
        }
        val lessonDays = remember(allLessons) { allLessons.map { it.date }.toSet() }
        val today = LocalDate.now(zone)
        val months = remember(byDay, lessonDays, today) {
            val current = YearMonth.from(today)
            val first = (byDay.keys + lessonDays).minOrNull()?.let(YearMonth::from)?.takeIf { it < current } ?: current
            generateSequence(first) { it.plusMonths(1) }.takeWhile { it <= current }.toList()
        }
        val pager = rememberPagerState(initialPage = months.lastIndex) { months.size }
        val scope = rememberCoroutineScope()
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, enabled = pager.currentPage > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_month))
                }
                Text(
                    monthTitle(months[pager.currentPage]),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    enabled = pager.currentPage < months.lastIndex,
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month)) }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HorizontalPager(pager, Modifier.fillMaxWidth()) { page ->
                MonthGrid(months[page], byDay, lessonDays, today, onOpenDay)
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    byDay: Map<LocalDate, List<Moment>>,
    lessonDays: Set<LocalDate>,
    today: LocalDate,
    onOpenDay: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        calendarCells(month).chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(0.75f)) {
                        if (date != null) DayCell(date, byDay[date].orEmpty(), date in lessonDays, date == today, date > today, onOpenDay)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, moments: List<Moment>, hasLessons: Boolean, isToday: Boolean, isFuture: Boolean, onOpenDay: (LocalDate) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val open = moments.isNotEmpty() || hasLessons
    val description = dayTitle(date)
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .then(if (open) Modifier.clickable { onOpenDay(date) } else Modifier)
            .semantics { contentDescription = description },
    ) {
        val first = moments.firstOrNull()
        if (first != null) MomentStack(moments)
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal),
            color = when {
                first != null -> Color.White
                isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.align(if (first != null) Alignment.TopStart else Alignment.Center).padding(4.dp),
        )
        if (first == null && hasLessons) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(5.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

/**
 * A day's moments as a small pile of photos: the first on top, the others tilted behind it so a piece of each shows.
 */
@Composable
private fun MomentStack(moments: List<Moment>) {
    val shown = moments.take(3)
    // Back to front: index 0 (the day's first moment) is drawn last, on top.
    shown.indices.reversed().forEach { i ->
        val behind = i > 0
        Box(
            Modifier
                .fillMaxSize()
                // A pile needs room to fan out: the photos shrink a little so the tilted ones peek past the edges.
                .padding(horizontal = if (shown.size > 1) 7.dp else 0.dp, vertical = if (shown.size > 1) 6.dp else 0.dp)
                .graphicsLayer {
                    rotationZ = when (i) { 1 -> 12f; 2 -> -12f; else -> 0f }
                    translationX = when (i) { 1 -> 5.dp.toPx(); 2 -> -5.dp.toPx(); else -> 0f }
                }
                .clip(RoundedCornerShape(8.dp))
                .then(if (shown.size > 1) Modifier.border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp)) else Modifier),
        ) {
            MomentImage(shown[i], maxPx = 200, modifier = Modifier.fillMaxSize())
            // Darker behind, so the top photo stands out; a light veil on top keeps the day number readable.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (behind) 0.35f else 0.15f)))
        }
    }
}

