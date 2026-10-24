package com.karvo.ui.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karvo.KarvoApp
import com.karvo.R
import com.karvo.domain.Moment
import com.karvo.domain.dayTitle
import com.karvo.domain.formatDuration
import com.karvo.domain.momentsOn
import com.karvo.ui.components.LessonRow
import com.karvo.ui.theme.groupedShape
import com.karvo.ui.theme.tileColor
import java.time.LocalDate

/** Days a swipe can reach either side of the one opened: far more than anyone will swipe. */
private const val DAY_PAGES = 20_000

/**
 * One day at a glance: its moments to swipe through, then the lessons taught and the hours. Days sit side by side
 * like the photos, so a swipe slides in the next or previous day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayRecapScreen(
    date: LocalDate,
    onBack: () -> Unit,
    onOpenMoment: (day: LocalDate, index: Int) -> Unit,
    onEditLesson: (Long) -> Unit,
    onOpenCamera: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as KarvoApp).container
    val zone = rememberZone()
    val allMoments = rememberMoments()
    val allLessons by container.lessonRepository.observeLessons().collectAsStateWithLifecycle(initialValue = emptyList())
    val days = rememberPagerState(initialPage = DAY_PAGES / 2) { DAY_PAGES }
    fun dayAt(page: Int): LocalDate = date.plusDays((page - DAY_PAGES / 2).toLong())

    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { Text(dayTitle(dayAt(days.currentPage))) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        HorizontalPager(days, Modifier.padding(padding).fillMaxSize(), key = { it }) { page ->
            val day = dayAt(page)
            val moments = allMoments?.let { momentsOn(day, it, zone) }
            val lessons = remember(allLessons, day) { allLessons.filter { it.date == day } }
            val isToday = day == LocalDate.now(zone)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "moments") {
                    when {
                        moments == null -> Unit
                        moments.isEmpty() -> Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(stringResource(R.string.no_moment_this_day), textAlign = TextAlign.Center)
                            if (isToday) Button(onClick = onOpenCamera) { Text(stringResource(R.string.take_moment)) }
                        }
                        else -> MomentPager(moments, playing = days.currentPage == page, onOpenMoment = { onOpenMoment(day, it) })
                    }
                }
                item(key = "summary") {
                    val minutes = lessons.sumOf { it.durationMinutes.toLong() }
                    Text(
                        if (lessons.isEmpty()) stringResource(R.string.no_lessons_this_day)
                        else pluralStringResource(R.plurals.lesson_count, lessons.size, lessons.size) + "  ·  " + formatDuration(minutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                    LessonRow(
                        lesson,
                        onClick = { onEditLesson(lesson.id) },
                        containerColor = tileColor(),
                        modifier = Modifier.padding(horizontal = 16.dp).clip(groupedShape(index, lessons.size)),
                    )
                }
            }
        }
    }
}

/** [playing]: whether this day is the one on screen, so videos on the days either side stay still. */
@Composable
private fun MomentPager(moments: List<Moment>, playing: Boolean, onOpenMoment: (index: Int) -> Unit) {
    val pager = rememberPagerState { moments.size }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(pager, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 32.dp), pageSpacing = 12.dp) { page ->
            val moment = moments[page]
            val label = stringResource(if (moment.isVideo) R.string.video else R.string.photo)
            Box(
                Modifier
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClickLabel = label) { onOpenMoment(page) },
            ) {
                MomentImage(moment, maxPx = 1080, modifier = Modifier.fillMaxSize())
                if (moment.isVideo) {
                    // Plays muted and on loop while it's the page on screen; tapping opens it with sound.
                    LoopingVideo(moment.uri, playing = playing && pager.currentPage == page, modifier = Modifier.fillMaxSize())
                    Icon(
                        Icons.Filled.Videocam,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .padding(5.dp),
                    )
                }
            }
        }
        if (moments.size > 1) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(moments.size) { i ->
                    Box(
                        Modifier.size(7.dp).background(
                            if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                    )
                }
            }
        }
    }
}
