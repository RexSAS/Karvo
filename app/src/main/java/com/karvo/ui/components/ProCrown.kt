package com.karvo.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.karvo.R

/** The yellow Pro crown. */
@Composable
fun ProCrown(modifier: Modifier = Modifier, contentDescription: String? = stringResource(R.string.pro_required)) {
    Icon(painterResource(R.drawable.ic_crown), contentDescription = contentDescription, tint = proGold(), modifier = modifier)
}

/** Crown gold; on light surfaces it darkens to keep text at 4.5:1. */
@Composable
fun proGold(): Color = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFF946B00) else Color(0xFFFFC107)
