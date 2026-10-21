package com.masterski.ui.overview

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.masterski.domain.StudentStat
import com.masterski.domain.earningsLabel
import com.masterski.domain.formatDuration

/** A student with their 1-based place in the ranking. */
data class Ranked(val rank: Int, val student: StudentStat)

/** Podium places in display order: 2nd, 1st, 3rd (missing places are left out). */
fun podiumOrder(top: List<StudentStat>): List<Ranked> =
    listOf(2, 1, 3).filter { it <= top.size }.map { Ranked(it, top[it - 1]) }

/** Places after the podium (4th onwards). */
fun runnersUp(top: List<StudentStat>): List<Ranked> =
    top.drop(3).mapIndexed { i, student -> Ranked(i + 4, student) }

private val Gold = Color(0xFFE8B964)
private val Silver = Color(0xFFC9D3E0)
private val Bronze = Color(0xFFCD8B5A)
private val White = Color(0xFFFFFFFF)

private fun medalColor(rank: Int) = when (rank) {
    1 -> Gold
    2 -> Silver
    3 -> Bronze
    else -> White
}

private fun blockHeight(rank: Int): Dp = when (rank) {
    1 -> 96.dp
    2 -> 72.dp
    else -> 56.dp
}

/** Podium for the top 3 plus compact rows for 4th and 5th. Money shows only when [revealed]. */
@Composable
fun TopStudentsPodium(top: List<StudentStat>, revealed: Boolean, rateSet: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            podiumOrder(top).forEach { PodiumPlace(it, revealed, rateSet, Modifier.weight(1f)) }
        }
        val rest = runnersUp(top)
        if (rest.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(top = 0.dp), color = MaterialTheme.colorScheme.outlineVariant)
            rest.forEach { RunnerUpRow(it, revealed, rateSet) }
        }
    }
}

@Composable
private fun PodiumPlace(place: Ranked, revealed: Boolean, rateSet: Boolean, modifier: Modifier) {
    val color = medalColor(place.rank)
    var raised by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { raised = true }
    val height by animateDpAsState(if (raised) blockHeight(place.rank) else 0.dp, tween(600), label = "podium")

    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.MilitaryTech,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(if (place.rank == 1) 44.dp else 34.dp),
        )
        Text(
            place.student.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(formatDuration(place.student.minutes), style = MaterialTheme.typography.bodyMedium)
        if (rateSet) {
            Text(
                earningsLabel(place.student.earningsCents, revealed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(color.copy(alpha = 0.22f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (height > 32.dp) {
                Text(
                    place.rank.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = color,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun RunnerUpRow(place: Ranked, revealed: Boolean, rateSet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = White, modifier = Modifier.size(24.dp))
        Text(
            place.rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp).padding(start = 4.dp),
        )
        Text(
            place.student.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDuration(place.student.minutes), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            if (rateSet) {
                Text(
                    earningsLabel(place.student.earningsCents, revealed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                )
            }
        }
    }
}
