package com.karvo.ui.report

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karvo.BuildConfig
import com.karvo.R
import com.karvo.data.ReportStatus
import com.karvo.data.SentReport
import com.karvo.data.TooManyReportsException
import com.karvo.data.fetchReportStatus
import com.karvo.data.rememberReport
import com.karvo.data.sendReport
import com.karvo.data.sentReports
import com.karvo.ui.components.EmptyState
import com.karvo.ui.components.accentGold
import com.karvo.ui.theme.AlpineTile
import com.karvo.ui.theme.headerFade
import com.karvo.ui.theme.underBars
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Same public address as PRIVACY.md. */
private const val SUPPORT_EMAIL = "andreafaveri@rxsas.it"

private fun reportApp() = "Karvo ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR})"
private fun reportDevice() = "Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

/** Report a problem (sent as a public GitHub issue through the relay) and, below, the reports sent from this phone with their answers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(onBack: () -> Unit, showMessage: (String) -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val reports = remember { sentReports(context).toMutableStateList() }

    fun send() {
        when {
            title.trim().length < 3 -> error = R.string.report_title_short
            text.isBlank() -> error = R.string.report_text_empty
            else -> {
                error = null
                sending = true
                scope.launch {
                    try {
                        val report = SentReport(sendReport(title.trim(), text.trim(), reportApp(), reportDevice()), title.trim())
                        rememberReport(context, report)
                        reports.add(0, report)
                        title = ""
                        text = ""
                        showMessage(context.getString(R.string.report_sent, report.number))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: TooManyReportsException) {
                        error = R.string.report_too_many
                    } catch (_: Exception) {
                        error = R.string.report_failed
                    } finally {
                        sending = false
                    }
                }
            }
        }
    }

    fun email() {
        // Fallback when sending fails: the mail app, with the same version and phone lines.
        val body = text.trim().ifBlank { context.getString(R.string.report_problem_body) } + "\n\n---\n" + reportApp() + "\n" + reportDevice()
        val subject = "Karvo – " + title.trim().ifBlank { context.getString(R.string.report_problem) }
        try {
            uriHandler.openUri("mailto:$SUPPORT_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        } catch (_: Exception) {
            showMessage(context.getString(R.string.no_email_app, SUPPORT_EMAIL))
        }
    }

    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.headerFade(),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.report_problem)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = underBars(padding, around = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.report_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                AlpineTile(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(120); error = null },
                            label = { Text(stringResource(R.string.report_title)) },
                            singleLine = true,
                            enabled = !sending,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it.take(4000); error = null },
                            label = { Text(stringResource(R.string.report_text)) },
                            placeholder = { Text(stringResource(R.string.report_problem_body)) },
                            minLines = 4,
                            enabled = !sending,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.report_public),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                        Button(onClick = ::send, enabled = !sending, modifier = Modifier.fillMaxWidth()) {
                            if (sending) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (sending) R.string.report_sending else R.string.report_send))
                        }
                        TextButton(onClick = ::email, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(stringResource(R.string.report_email))
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.my_reports).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                )
            }
            if (reports.isEmpty()) {
                item { EmptyState(stringResource(R.string.my_reports_empty), Modifier.padding(top = 8.dp).fillMaxWidth()) }
            }
            items(reports.take(10), key = { it.number }) { report -> ReportCard(report) }
        }
    }
}

@Composable
private fun ReportCard(report: SentReport) {
    var status by remember { mutableStateOf<ReportStatus?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(report.number) {
        try {
            status = fetchReportStatus(report.number)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        }
    }
    AlpineTile(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(report.title, style = MaterialTheme.typography.titleMedium)
                    Text("#${report.number}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                status?.let { StatusChip(it.open) }
            }
            val s = status
            when {
                failed -> Text(stringResource(R.string.my_reports_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                s == null -> Text(stringResource(R.string.report_loading), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.replies.isEmpty() -> Text(stringResource(R.string.report_no_replies), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> s.replies.forEach { reply ->
                    val fromKarvo = reply.author == "RexSAS"
                    Surface(
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                        color = if (fromKarvo) accentGold().copy(alpha = 0.14f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                (if (fromKarvo) "Karvo" else reply.author) + " · " + reply.date,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (fromKarvo) accentGold() else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(reply.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(open: Boolean) {
    val color = if (open) accentGold() else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(shape = CircleShape, color = color.copy(alpha = 0.16f)) {
        Text(
            stringResource(if (open) R.string.report_open else R.string.report_closed),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
