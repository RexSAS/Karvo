package com.karvo.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import com.karvo.R
import com.karvo.domain.Badge

@StringRes
fun Badge.titleRes(): Int = when (this) {
    Badge.FIRST_TRACKS -> R.string.badge_first_tracks
    Badge.SNOWPLOUGH -> R.string.badge_snowplough
    Badge.PARALLEL -> R.string.badge_parallel
    Badge.CARVER -> R.string.badge_carver
    Badge.MOGULS -> R.string.badge_moguls
    Badge.POWDER -> R.string.badge_powder
    Badge.LEGEND -> R.string.badge_legend
}

/** Bronze, silver, gold; the last one in glacier ice. */
private fun Badge.metal(): Color = when (this) {
    Badge.FIRST_TRACKS, Badge.SNOWPLOUGH -> Color(0xFFC98A55)
    Badge.PARALLEL, Badge.CARVER -> Color(0xFFC3CCD6)
    Badge.MOGULS, Badge.POWDER -> Color(0xFFF4C06A)
    Badge.LEGEND -> Color(0xFF9FD8F5)
}

/** A round medal with a mountain on it; [badge] null draws the empty slot as a dashed ring. Decorative: callers describe it. */
@Composable
fun BadgeMedal(badge: Badge?, modifier: Modifier = Modifier) {
    val ring = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        if (badge == null) {
            val w = r * 0.07f
            drawCircle(ring.copy(alpha = 0.6f), r - w, c, style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.22f, r * 0.16f))))
            return@Canvas
        }
        val metal = badge.metal()
        val dark = metal.copy(red = metal.red * 0.62f, green = metal.green * 0.62f, blue = metal.blue * 0.62f)
        drawCircle(dark, r, c)
        drawCircle(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f).compositeOver(metal), metal, dark), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)), r * 0.86f, c)
        // Two peaks, the front one with a snow cap.
        val ink = Color(0xFF1B2433).copy(alpha = 0.78f)
        val base = c.y + r * 0.38f
        drawPath(Path().apply {
            moveTo(c.x - r * 0.62f, base); lineTo(c.x - r * 0.22f, c.y - r * 0.12f); lineTo(c.x + r * 0.05f, base); close()
        }, ink.copy(alpha = 0.5f))
        val peak = Offset(c.x + r * 0.1f, c.y - r * 0.45f)
        drawPath(Path().apply {
            moveTo(c.x - r * 0.4f, base); lineTo(peak.x, peak.y); lineTo(c.x + r * 0.62f, base); close()
        }, ink)
        drawPath(Path().apply {
            moveTo(peak.x, peak.y); lineTo(peak.x + r * 0.17f, peak.y + r * 0.27f); lineTo(peak.x + r * 0.05f, peak.y + r * 0.21f)
            lineTo(peak.x - r * 0.04f, peak.y + r * 0.28f); lineTo(peak.x - r * 0.16f, peak.y + r * 0.27f); close()
        }, Color.White)
    }
}

