package com.karvo.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import com.karvo.R
import com.karvo.domain.PaidStatus

/** Amber for "partly paid" and "not paid"; on light surfaces it darkens to keep text at 4.5:1. */
@Composable
fun partlyPaidColor(): Color = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFF9A5B00) else Color(0xFFF3AE72)

/** "✓ Paid" in gold or "Partly paid" in amber; nothing for unpaid months, so the list stays calm. */
@Composable
fun PaidBadge(status: PaidStatus, modifier: Modifier = Modifier) {
    when (status) {
        PaidStatus.PAID -> Text(
            "✓ " + stringResource(R.string.paid),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier,
        )
        PaidStatus.PARTLY_PAID -> Text(
            stringResource(R.string.partly_paid),
            style = MaterialTheme.typography.labelMedium,
            color = partlyPaidColor(),
            modifier = modifier,
        )
        PaidStatus.UNPAID -> Unit
    }
}
