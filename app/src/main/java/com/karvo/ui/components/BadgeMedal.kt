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

/** A crest-shaped badge with a mountain on it; [badge] null draws the empty slot as a dashed outline. Decorative: callers describe it. */
@Composable
fun BadgeMedal(badge: Badge?, modifier: Modifier = Modifier) {
    val ring = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val h = size.minDimension
        val half = h * 0.43f // the shield is a little narrower than tall
        val c = center
        /** The shield scaled by [k] around its centre: a raised point on top, straight sides curving into a point below. */
        fun shield(k: Float) = Path().apply {
            fun x(u: Float) = c.x + u * half * k
            fun y(v: Float) = c.y + v * h * k
            moveTo(x(-1f), y(-0.38f))
            quadraticTo(x(-0.45f), y(-0.38f), x(0f), y(-0.5f))
            quadraticTo(x(0.45f), y(-0.38f), x(1f), y(-0.38f))
            lineTo(x(1f), y(0.02f))
            cubicTo(x(1f), y(0.3f), x(0.5f), y(0.42f), x(0f), y(0.5f))
            cubicTo(x(-0.5f), y(0.42f), x(-1f), y(0.3f), x(-1f), y(0.02f))
            close()
        }
        if (badge == null) {
            val w = h * 0.035f
            drawPath(shield(0.93f), ring.copy(alpha = 0.6f), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(h * 0.1f, h * 0.07f))))
            return@Canvas
        }
        val metal = badge.metal()
        val dark = metal.copy(red = metal.red * 0.62f, green = metal.green * 0.62f, blue = metal.blue * 0.62f)
        drawPath(shield(1f), dark)
        drawPath(shield(0.84f), Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.9f).compositeOver(metal), metal, dark),
            Offset(c.x - half, c.y - h / 2), Offset(c.x + half, c.y + h / 2),
        ))
        // Two peaks, the front one with a snow cap.
        val r = half * 0.9f
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
