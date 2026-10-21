package com.karvo.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.karvo.R
import com.karvo.data.backup.Backup
import com.karvo.data.backup.BackupReason
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "21 Oct 2026, 14:05" in the user's time zone. */
fun backupDateText(backup: Backup, zoneId: String, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale).format(backup.createdAt.atZone(ZoneId.of(zoneId)))

@Composable
fun backupReasonText(reason: BackupReason): String = when (reason) {
    BackupReason.Manual -> stringResource(R.string.backup_reason_manual)
    BackupReason.BeforeRestore -> stringResource(R.string.backup_reason_restore)
    BackupReason.BeforeImport -> stringResource(R.string.backup_reason_import)
    is BackupReason.BeforeUpdate -> stringResource(R.string.backup_reason_update, reason.version)
}

@Composable
fun backupCountText(backup: Backup): String =
    if (backup.lessonCount < 0) {
        stringResource(R.string.backup_unreadable)
    } else {
        pluralStringResource(R.plurals.lesson_count, backup.lessonCount, backup.lessonCount)
    }
