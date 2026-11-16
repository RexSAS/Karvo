package com.karvo.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.karvo.R
import com.karvo.domain.Badge

@StringRes
fun Badge.titleRes(): Int = when (this) {
    Badge.KARVER -> R.string.badge_karver
    Badge.FIRST_TRACKS -> R.string.badge_first_tracks
    Badge.BUNNY_HILL -> R.string.badge_bunny_hill
    Badge.SNOWPLOUGH -> R.string.badge_snowplough
    Badge.PARALLEL -> R.string.badge_parallel
    Badge.RED_RUN -> R.string.badge_red_run
    Badge.EDGE_MASTER -> R.string.badge_edge_master
    Badge.BLACK_RUN -> R.string.badge_black_run
    Badge.MOGULS -> R.string.badge_moguls
    Badge.OFF_PISTE -> R.string.badge_off_piste
    Badge.POWDER -> R.string.badge_powder
    Badge.AVALANCHE -> R.string.badge_avalanche
    Badge.SUMMIT -> R.string.badge_summit
    Badge.LEGEND -> R.string.badge_legend
}

/** A metal's face, its highlight and its shade. */
private class Metal(val base: Color, val light: Color, val dark: Color)

/** Bronze, silver, gold, amethyst; the legends in glacier ice. */
private val metals = listOf(
    Metal(Color(0xFFC98A55), Color(0xFFF6D6B4), Color(0xFF6E4120)),
    Metal(Color(0xFFC3CCD6), Color(0xFFF6F9FC), Color(0xFF5F6B7A)),
    Metal(Color(0xFFF4C06A), Color(0xFFFFF3CF), Color(0xFF94621A)),
    Metal(Color(0xFFB9A3FF), Color(0xFFF1EBFF), Color(0xFF5B46A8)),
    Metal(Color(0xFF8FD3F7), Color(0xFFEFFBFF), Color(0xFF2F6FAE)),
)
private val Ink = Color(0xFF1B2433)

/** The badge as a still picture, e.g. for a notification. */
fun badgeBitmap(badge: Badge, px: Int): ImageBitmap = ImageBitmap(px, px).also { bitmap ->
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, GraphicsCanvas(bitmap), Size(px.toFloat(), px.toFloat())) {
        drawBadge(badge, Color.Gray)
    }
}

/** The rank emblem for [badge]; null draws the empty slot as a dashed outline. Decorative: callers describe it. */
@Composable
fun BadgeMedal(badge: Badge?, modifier: Modifier = Modifier) {
    val ring = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) { drawBadge(badge, ring) }
}

/**
 * A rank emblem, as in competitive games: a faceted crest with the mountain and rank chevrons (the step inside the
 * metal, 1 to 3). Each metal adds to the one before: wings from gold, a crown from amethyst, light rays for the ice
 * legends. [glint] 0..1 sends a shine across it (outside that range: none); [spin] turns the rays, in degrees.
 */
fun DrawScope.drawBadge(badge: Badge?, emptyColor: Color, glint: Float = -1f, spin: Float = 0f) {
    val s = size.minDimension
    val cx = center.x
    val cy = center.y + s * 0.03f
    val h = s * 0.62f
    val half = h * 0.42f
    fun emblem(k: Float) = Path().apply {
        fun x(u: Float) = cx + u * half * k
        fun y(v: Float) = cy + v * h * k
        moveTo(x(0f), y(-0.5f)); lineTo(x(1f), y(-0.3f)); lineTo(x(1f), y(0.14f))
        lineTo(x(0f), y(0.5f)); lineTo(x(-1f), y(0.14f)); lineTo(x(-1f), y(-0.3f)); close()
    }
    fun poly(p: List<Offset>) = Path().apply { p.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; close() }

    if (badge == null) {
        drawPath(emblem(0.92f), emptyColor.copy(alpha = 0.6f),
            style = Stroke(s * 0.028f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.08f, s * 0.055f))))
        return
    }
    val tier = badge.tier
    val m = metals[tier]

    if (tier == 4) {
        val r = s * 0.5f
        for (i in 0 until 14) rotate(spin + i * 360f / 14, Offset(cx, cy)) {
            drawPath(poly(listOf(Offset(cx, cy), Offset(cx + r, cy - r * 0.09f), Offset(cx + r, cy + r * 0.09f))),
                Brush.linearGradient(listOf(m.base.copy(alpha = 0.4f), m.base.copy(alpha = 0f)), Offset(cx, cy), Offset(cx + r, cy)))
        }
        drawCircle(Brush.radialGradient(listOf(m.base.copy(alpha = 0.5f), m.base.copy(alpha = 0f)), Offset(cx, cy), s * 0.48f), s * 0.48f, Offset(cx, cy))
    }
    if (tier >= 2) {
        // Wings: angled feathers out of both sides, longer from amethyst.
        val n = if (tier >= 3) 4 else 3
        val len = s * (if (tier >= 3) 0.27f else 0.2f)
        for (side in listOf(-1f, 1f)) for (i in 0 until n) {
            val x0 = cx + side * half * 0.9f
            val y0 = cy - h * 0.22f + i * h * 0.13f
            val l = len * (1 - i * 0.16f)
            val fw = h * 0.1f
            val tip = Offset(x0 + side * l, y0 - l * 0.5f)
            val feather = poly(listOf(Offset(x0, y0), tip, Offset(x0 + side * l * 0.86f, y0 - l * 0.5f + fw), Offset(x0, y0 + fw)))
            drawPath(feather, Brush.linearGradient(listOf(m.dark, m.light), Offset(x0, y0), tip))
            drawPath(feather, m.dark, style = Stroke(s * 0.008f))
        }
    }
    if (tier >= 3) {
        // A gold crown with a ruby.
        val y0 = cy - h * 0.5f
        val w = half * 0.62f
        val crown = poly(listOf(
            Offset(cx - w, y0 + h * 0.04f), Offset(cx - w, y0 - h * 0.1f), Offset(cx - w * 0.5f, y0 - h * 0.02f), Offset(cx, y0 - h * 0.16f),
            Offset(cx + w * 0.5f, y0 - h * 0.02f), Offset(cx + w, y0 - h * 0.1f), Offset(cx + w, y0 + h * 0.04f),
        ))
        drawPath(crown, metals[2].base)
        drawPath(crown, metals[2].dark, style = Stroke(s * 0.01f))
        drawCircle(Color(0xFFE9405B), s * 0.016f, Offset(cx, y0 - h * 0.07f))
    }

    // The rim: a lit left facet and a shaded right one; then the face.
    val outer = emblem(1f)
    drawPath(outer, m.dark)
    clipPath(outer) { drawRect(m.base, Offset(cx - s, 0f), Size(s, size.height)) }
    val face = emblem(0.8f)
    drawPath(face, Brush.verticalGradient(0f to m.light, 0.55f to m.base, 1f to m.dark, startY = cy - h / 2, endY = cy + h / 2))
    drawPath(face, Color.White.copy(alpha = 0.5f), style = Stroke(s * 0.01f))

    // Two peaks, the front one with a snow cap.
    val r = half * 0.78f
    val my = cy - h * 0.1f
    val base = my + r * 0.38f
    drawPath(poly(listOf(Offset(cx - r * 0.62f, base), Offset(cx - r * 0.22f, my - r * 0.12f), Offset(cx + r * 0.05f, base))), Ink.copy(alpha = 0.42f))
    val peak = Offset(cx + r * 0.1f, my - r * 0.45f)
    drawPath(poly(listOf(Offset(cx - r * 0.4f, base), peak, Offset(cx + r * 0.62f, base))), Ink.copy(alpha = 0.82f))
    drawPath(poly(listOf(peak, Offset(peak.x + r * 0.17f, peak.y + r * 0.27f), Offset(peak.x + r * 0.05f, peak.y + r * 0.21f),
        Offset(peak.x - r * 0.04f, peak.y + r * 0.28f), Offset(peak.x - r * 0.16f, peak.y + r * 0.27f))), Color.White)

    // Rank chevrons: the step inside the metal, narrowing towards the point.
    for (i in 0..badge.step) {
        val y = cy + h * 0.08f + i * h * 0.065f
        val w = half * (0.42f - i * 0.08f)
        drawPath(poly(listOf(Offset(cx - w, y), Offset(cx, y + h * 0.05f), Offset(cx + w, y), Offset(cx + w, y + h * 0.03f),
            Offset(cx, y + h * 0.08f), Offset(cx - w, y + h * 0.03f))), Ink.copy(alpha = 0.78f))
    }

    if (glint in 0f..1f) clipPath(outer) {
        val x = center.x - s / 2 + (glint * 2.2f - 0.6f) * s
        drawRect(Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0f)),
            Offset(x - s * 0.18f, cy - h / 2), Offset(x + s * 0.18f, cy - h / 2 + s * 0.3f),
        ))
    }
}
