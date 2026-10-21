package com.karvo.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.karvo.R
import com.karvo.domain.PaidStatus

/** Amber that reads on both the Alpine Night navy and Classic's light surfaces. */
val PartlyPaidColor = Color(0xFFE09A2B)

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
            color = PartlyPaidColor,
            modifier = modifier,
        )
        PaidStatus.UNPAID -> Unit
    }
}
