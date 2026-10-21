package com.karvo.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karvo.R
import com.karvo.domain.SeasonStats
import com.karvo.domain.earningsLabel
import com.karvo.domain.formatDuration
import com.karvo.ui.theme.AlpineTile

/** "+12h" / "−3h 30m": the difference between two totals, with its sign. */
fun signedDuration(minutes: Long): String = (if (minutes < 0) "−" else "+") + formatDuration(kotlin.math.abs(minutes))

/** Folded list of every season with lessons; starts folded each time the app opens (state is not saved). */
@Composable
fun SeasonsSection(seasons: SeasonStats, revealed: Boolean, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    AlpineTile(modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.seasons_header, seasons.all.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (open) R.string.collapse else R.string.expand),
                )
            }
            if (open) {
                seasons.all.forEachIndexed { index, total ->
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(total.season.label(), style = MaterialTheme.typography.titleMedium)
                            Text(formatDuration(total.minutes), style = MaterialTheme.typography.titleMedium)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                pluralStringResource(R.plurals.lesson_count, total.lessonCount, total.lessonCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // Seasons are newest first: the one before this is the next in the list.
                            seasons.all.getOrNull(index + 1)?.let { previous ->
                                Text(
                                    signedDuration(total.minutes - previous.minutes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                earningsLabel(total.earningsCents, revealed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                        }
                    }
                }
            }
        }
    }
}
