package com.karvo.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.karvo.R
import com.karvo.domain.Badge
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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

/**
 * A crest-shaped badge with a mountain on it; [badge] null draws the empty slot as a dashed outline. Each badge adds
 * something to the one before: stars, a bright trim, laurels, a gem on top, and for the last one a glow.
 * Decorative: callers describe it.
 */
@Composable
fun BadgeMedal(badge: Badge?, modifier: Modifier = Modifier) {
    val ring = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val box = size.minDimension
        val h = box * 0.78f // the shield; the rest of the box is room for laurels, gem and glow
        val half = h * 0.43f
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
            drawPath(shield(0.93f), ring.copy(alpha = 0.6f), style = Stroke(box * 0.03f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(box * 0.08f, box * 0.055f))))
            return@Canvas
        }
        val level = badge.ordinal
        val metal = badge.metal()
        val dark = metal.copy(red = metal.red * 0.62f, green = metal.green * 0.62f, blue = metal.blue * 0.62f)
        val light = Color.White.copy(alpha = 0.9f).compositeOver(metal)

        if (badge == Badge.LEGEND) {
            drawCircle(Brush.radialGradient(listOf(metal.copy(alpha = 0.55f), metal.copy(alpha = 0f)), c, box / 2), box / 2, c)
            listOf(Offset(-0.4f, -0.36f), Offset(0.42f, -0.3f), Offset(-0.44f, 0.3f), Offset(0.4f, 0.38f)).forEachIndexed { n, o ->
                drawPath(sparkle(Offset(c.x + o.x * box, c.y + o.y * box), box * (if (n % 2 == 0) 0.06f else 0.045f)), Color.White)
            }
        }
        if (level >= 3) {
            // Laurel branches hugging the shield's sides, bigger from the gold badges on.
            val leaves = 5
            val leaf = if (level >= 4) 1f else 0.72f
            val ax = half + box * 0.07f
            val ay = h * 0.42f
            for (side in listOf(-1f, 1f)) for (n in 0 until leaves) {
                val t = Math.toRadians(30.0 + n * (100.0 / (leaves - 1)))
                val at = Offset(c.x + side * ax * sin(t).toFloat(), c.y + ay * cos(t).toFloat())
                val angle = Math.toDegrees(atan2(-ay * sin(t), side * ax * cos(t))).toFloat()
                rotate(angle, at) {
                    drawOval(metal, Offset(at.x - box * 0.07f * leaf, at.y - box * 0.028f * leaf), Size(box * 0.14f * leaf, box * 0.056f * leaf))
                }
            }
        }
        drawPath(shield(1f), dark)
        drawPath(shield(0.84f), Brush.linearGradient(listOf(light, metal, dark), Offset(c.x - half, c.y - h / 2), Offset(c.x + half, c.y + h / 2)))
        if (level >= 2) drawPath(shield(0.9f), light.copy(alpha = 0.7f), style = Stroke(box * 0.012f))

        // Two peaks, the front one with a snow cap.
        val r = half * 0.9f
        val ink = Color(0xFF1B2433).copy(alpha = 0.78f)
        val my = c.y - h * 0.05f
        val base = my + r * 0.38f
        drawPath(Path().apply {
            moveTo(c.x - r * 0.62f, base); lineTo(c.x - r * 0.22f, my - r * 0.12f); lineTo(c.x + r * 0.05f, base); close()
        }, ink.copy(alpha = 0.5f))
        val peak = Offset(c.x + r * 0.1f, my - r * 0.45f)
        drawPath(Path().apply {
            moveTo(c.x - r * 0.4f, base); lineTo(peak.x, peak.y); lineTo(c.x + r * 0.62f, base); close()
        }, ink)
        drawPath(Path().apply {
            moveTo(peak.x, peak.y); lineTo(peak.x + r * 0.17f, peak.y + r * 0.27f); lineTo(peak.x + r * 0.05f, peak.y + r * 0.21f)
            lineTo(peak.x - r * 0.04f, peak.y + r * 0.28f); lineTo(peak.x - r * 0.16f, peak.y + r * 0.27f); close()
        }, Color.White)

        // Stars under the mountain: one per badge after the first, up to three.
        val stars = minOf(level, 3)
        val gap = h * 0.13f
        for (n in 0 until stars) {
            drawPath(star(Offset(c.x + (n - (stars - 1) / 2f) * gap, c.y + h * 0.22f), h * 0.055f), ink)
        }
        if (level >= 5) {
            // A gem set on the shield's top point.
            val g = Offset(c.x, c.y - h * 0.5f)
            val gr = box * 0.075f
            val gem = Path().apply { moveTo(g.x, g.y - gr); lineTo(g.x + gr * 0.75f, g.y); lineTo(g.x, g.y + gr); lineTo(g.x - gr * 0.75f, g.y); close() }
            drawPath(gem, Brush.linearGradient(listOf(Color.White, Color(0xFF9FD8F5), Color(0xFF3B7BB8)), Offset(g.x - gr, g.y - gr), Offset(g.x + gr, g.y + gr)))
            drawPath(gem, dark, style = Stroke(box * 0.012f))
        }
    }
}

private fun star(c: Offset, r: Float) = Path().apply {
    for (k in 0 until 10) {
        val a = Math.PI / 5 * k - Math.PI / 2
        val rr = if (k % 2 == 0) r else r * 0.45f
        val x = c.x + rr * cos(a).toFloat()
        val y = c.y + rr * sin(a).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** A four-point twinkle. */
private fun sparkle(c: Offset, r: Float) = Path().apply {
    val w = r * 0.22f
    moveTo(c.x, c.y - r); lineTo(c.x + w, c.y - w); lineTo(c.x + r, c.y); lineTo(c.x + w, c.y + w)
    lineTo(c.x, c.y + r); lineTo(c.x - w, c.y + w); lineTo(c.x - r, c.y); lineTo(c.x - w, c.y - w); close()
}
