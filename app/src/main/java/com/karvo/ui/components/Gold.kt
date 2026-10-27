package com.karvo.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Accent gold; on light surfaces it darkens to keep text at 4.5:1. */
@Composable
fun accentGold(): Color = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFF946B00) else Color(0xFFFFC107)
