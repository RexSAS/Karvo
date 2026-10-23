package com.karvo.ui.pro

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.karvo.R
import com.karvo.ui.components.ProCrown
import com.karvo.ui.components.proGold

private data class ProFeature(val icon: ImageVector, @StringRes val title: Int, @StringRes val body: Int)

private val features = listOf(
    ProFeature(Icons.Filled.Euro, R.string.pro_feature_earnings, R.string.pro_feature_earnings_body),
    ProFeature(Icons.Filled.TaskAlt, R.string.pro_feature_paid, R.string.pro_feature_paid_body),
    ProFeature(Icons.Filled.BarChart, R.string.pro_feature_stats, R.string.pro_feature_stats_body),
    ProFeature(Icons.Filled.AcUnit, R.string.pro_feature_seasons, R.string.pro_feature_seasons_body),
    ProFeature(Icons.Filled.Videocam, R.string.pro_feature_videos, R.string.pro_feature_videos_body),
    ProFeature(Icons.Filled.Restore, R.string.pro_feature_backups, R.string.pro_feature_backups_body),
    ProFeature(Icons.Filled.ImportExport, R.string.pro_feature_csv, R.string.pro_feature_csv_body),
    ProFeature(Icons.Filled.DarkMode, R.string.pro_feature_theme, R.string.pro_feature_theme_body),
    ProFeature(Icons.Filled.Fingerprint, R.string.pro_feature_fingerprint, R.string.pro_feature_fingerprint_body),
)

/** What Karvo Pro includes; informational only, codes are redeemed in Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.pro_page_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProCrown(Modifier.size(64.dp), contentDescription = null)
            Text(stringResource(R.string.pro_page_headline), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.pro_page_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            features.forEach { feature ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(feature.icon, contentDescription = null, tint = proGold()) },
                    headlineContent = { Text(stringResource(feature.title)) },
                    supportingContent = { Text(stringResource(feature.body)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                stringResource(R.string.pro_page_code_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
