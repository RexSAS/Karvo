package com.karvo.onboarding

import com.karvo.domain.SeasonWelcome
import com.karvo.domain.Season
import com.karvo.domain.formatDuration
import com.karvo.ui.components.partlyPaidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.karvo.BuildConfig
import com.karvo.R
import com.karvo.ui.components.Kevin
import com.karvo.ui.components.KevinPose
import com.karvo.ui.theme.KarvoMotion
import kotlinx.coroutines.delay

/** Fades and slides its content up after [delayMs]: the staggered entrance of the start screens. */
@Composable
private fun Entrance(delayMs: Int, content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(KarvoMotion.Medium, easing = KarvoMotion.Ease)) }
    androidx.compose.foundation.layout.Box(Modifier.graphicsLayer { alpha = p.value; translationY = (1 - p.value) * 14.dp.toPx() }) { content() }
}

/** First launch: Kevin comes out of the snow to say hello, a welcome, and the quick tour or Skip. */
@Composable
fun WelcomeDialog(onTour: () -> Unit, onSkip: () -> Unit) {
    Dialog(onDismissRequest = onSkip) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Kevin(KevinPose.Idle, Modifier.size(width = 150.dp, height = 145.dp))
                Entrance(500) { Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) }
                Entrance(580) { Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Entrance(660) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        TextButton(onClick = onSkip) { Text(stringResource(R.string.tour_skip)) }
                        Button(onClick = onTour) { Text(stringResource(R.string.welcome_tour)) }
                    }
                }
            }
        }
    }
}

/** Once after an update: what changed in this version, one line after the other. */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.whats_new_title, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.headlineSmall)
                stringArrayResource(R.array.whats_new_items).forEachIndexed { i, item ->
                    Entrance(150 + i * 90) { Text("•  $item", style = MaterialTheme.typography.bodyMedium) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
                }
            }
        }
    }
}

/** The first time Karvo opens in a new season: Kevin, a welcome, last season in one line, and its months still unpaid. */
@Composable
fun SeasonWelcomeDialog(card: SeasonWelcome, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Kevin(KevinPose.Idle, Modifier.size(width = 150.dp, height = 145.dp))
                Entrance(500) { Text(stringResource(R.string.season_welcome_title, card.season.label()), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) }
                Entrance(580) { Text(stringResource(R.string.season_welcome_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                card.last?.let { last ->
                    Entrance(660) {
                        Text(
                            stringResource(
                                R.string.season_welcome_last, last.season.label(), formatDuration(last.minutes),
                                pluralStringResource(R.plurals.lesson_count, last.lessonCount, last.lessonCount),
                                pluralStringResource(R.plurals.student_count, card.lastStudents, card.lastStudents),
                            ),
                            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        )
                    }
                }
                if (card.lastUnpaidMonths > 0) {
                    Entrance(740) {
                        Text(
                            pluralStringResource(R.plurals.season_welcome_unpaid, card.lastUnpaidMonths, card.lastUnpaidMonths, Season(card.season.startYear - 1, card.season.startMonth).label()),
                            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = partlyPaidColor(),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
                }
            }
        }
    }
}
