package com.karvo.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karvo.R
import com.karvo.domain.Badge
import com.karvo.domain.badgeProgress
import com.karvo.domain.formatDuration
import com.karvo.ui.components.BadgeMedal
import com.karvo.ui.components.accentGold
import com.karvo.ui.components.titleRes
import com.karvo.ui.theme.AlpineTile
import com.karvo.ui.theme.headerFade
import com.karvo.ui.theme.underBars

/** The season's badge big, how far the next one is, every badge, and the best badge of each season. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(viewModel: OverviewViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val progress = badgeProgress(state.badgeMinutes)
    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.headerFade(),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.badges)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        LazyColumn(Modifier.fillMaxSize(), contentPadding = underBars(padding, extra = 16.dp)) {
            item(key = "hero") {
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp).semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BadgeMedal(progress.earned, Modifier.size(200.dp))
                    Text(
                        progress.earned?.let { stringResource(it.titleRes()) } ?: stringResource(R.string.badge_none),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        listOfNotNull(
                            state.badgeSeason?.let { stringResource(R.string.overview_scope_season, it.label()) },
                            formatDuration(state.badgeMinutes),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "next") {
                AlpineTile(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val next = progress.next
                        Text(
                            if (next == null) stringResource(R.string.badge_all_earned)
                            else stringResource(R.string.badge_next, stringResource(next.titleRes()), formatDuration(next.minutes)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        LinearProgressIndicator(
                            progress = { progress.fraction },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = accentGold(),
                            drawStopIndicator = {},
                        )
                        if (next != null) Text(
                            stringResource(R.string.badge_to_go, formatDuration(progress.minutesToNext)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item(key = "ladder") {
                Section(stringResource(R.string.badges_all)) {
                    Badge.entries.forEachIndexed { i, badge ->
                        if (i > 0) Divider()
                        val earned = state.badgeMinutes >= badge.minutes
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp).semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // A locked badge shows faded, so the whole ladder can be seen ahead.
                            BadgeMedal(badge, Modifier.size(52.dp).alpha(if (earned) 1f else 0.3f))
                            Text(stringResource(badge.titleRes()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(formatDuration(badge.minutes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (earned) Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.badge_earned), tint = accentGold())
                        }
                    }
                }
            }
            state.seasons?.takeIf { it.all.isNotEmpty() }?.let { seasons ->
                item(key = "seasons") {
                    Section(stringResource(R.string.badges_by_season)) {
                        seasons.all.forEachIndexed { i, total ->
                            if (i > 0) Divider()
                            val best = badgeProgress(total.minutes).earned
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 10.dp).semantics(mergeDescendants = true) {},
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                BadgeMedal(best, Modifier.size(52.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(total.season.label(), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        best?.let { stringResource(it.titleRes()) } ?: stringResource(R.string.badge_none),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(formatDuration(total.minutes), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 8.dp))
    AlpineTile(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { content() }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
