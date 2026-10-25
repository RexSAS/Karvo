package com.karvo.data.moments

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** A rectangle turned by the template's tilt: its centre and size, in template pixels. */
data class TiltedBox(val cx: Float, val cy: Float, val width: Float, val height: Float)

/**
 * What a share template PNG says, read from its pixels (the rules are in docs/share-designs/README.md): the photo
 * window is the transparent area the frame encloses, the date spot is the solid magenta box, and the frame is tilted
 * like that box. [pixels] (ARGB) has the magenta box painted over in the frame's colour, ready to draw.
 */
class ShareTemplate(val pixels: IntArray, val width: Int, val height: Int, val tilt: Float, val window: TiltedBox, val date: TiltedBox?)

private fun alpha(c: Int) = c ushr 24
private fun isMagenta(c: Int) = alpha(c) > 200 && (c shr 16 and 0xFF) > 230 && (c shr 8 and 0xFF) < 40 && (c and 0xFF) > 230

fun readShareTemplate(source: IntArray, width: Int, height: Int): ShareTemplate {
    val pixels = source.copyOf()
    val size = width * height

    // Tilt: the long axis of the magenta box, from its second moments.
    var n = 0
    var sx = 0.0
    var sy = 0.0
    for (i in 0 until size) if (isMagenta(pixels[i])) { n++; sx += i % width; sy += i / width }
    var tilt = 0.0
    if (n > 0) {
        val mx = sx / n
        val my = sy / n
        var xx = 0.0
        var yy = 0.0
        var xy = 0.0
        for (i in 0 until size) if (isMagenta(pixels[i])) {
            val dx = i % width - mx
            val dy = i / width - my
            xx += dx * dx; yy += dy * dy; xy += dx * dy
        }
        tilt = 0.5 * atan2(2 * xy, xx - yy)
    }

    // Outside: transparent pixels reachable from the edges. The transparent pixels left over are the window.
    val outside = BooleanArray(size)
    val queue = IntArray(size)
    var head = 0
    var tail = 0
    fun visit(i: Int) {
        if (!outside[i] && alpha(pixels[i]) < 128) { outside[i] = true; queue[tail++] = i }
    }
    for (x in 0 until width) { visit(x); visit(size - width + x) }
    for (y in 0 until height) { visit(y * width); visit(y * width + width - 1) }
    while (head < tail) {
        val i = queue[head++]
        val x = i % width
        if (x > 0) visit(i - 1)
        if (x < width - 1) visit(i + 1)
        if (i >= width) visit(i - width)
        if (i < size - width) visit(i + width)
    }
    val window = tiltedBox(width, size, tilt) { alpha(pixels[it]) < 128 && !outside[it] }
        ?: throw IllegalArgumentException("The share template has no photo window inside its frame")
    val date = if (n > 0) tiltedBox(width, size, tilt) { isMagenta(pixels[it]) } else null

    if (date != null) {
        // Paint the box over with the frame colour just below it, plus 2 px of its anti-aliased edge.
        val below = Math.round(date.cx - sin(tilt) * (date.height / 2 + 6)).toInt() +
            Math.round(date.cy + cos(tilt) * (date.height / 2 + 6)).toInt() * width
        val paper = pixels[below.coerceIn(0, size - 1)]
        val box = BooleanArray(size) { isMagenta(pixels[it]) }
        for (i in 0 until size) if (box[i]) {
            val x = i % width
            val y = i / width
            for (yy in maxOf(0, y - 2)..minOf(height - 1, y + 2)) for (xx in maxOf(0, x - 2)..minOf(width - 1, x + 2)) {
                pixels[yy * width + xx] = paper
            }
        }
    }
    return ShareTemplate(pixels, width, height, Math.toDegrees(tilt).toFloat(), window, date)
}

/** The smallest box, turned by [tilt], around the pixels [inside] picks; null if it picks none. */
private inline fun tiltedBox(width: Int, size: Int, tilt: Double, inside: (Int) -> Boolean): TiltedBox? {
    val cos = cos(tilt)
    val sin = sin(tilt)
    var minU = Double.MAX_VALUE
    var maxU = -Double.MAX_VALUE
    var minV = Double.MAX_VALUE
    var maxV = -Double.MAX_VALUE
    for (i in 0 until size) if (inside(i)) {
        // Pixel centres, measured along the tilted axes.
        val x = i % width + 0.5
        val y = i / width + 0.5
        val u = x * cos + y * sin
        val v = -x * sin + y * cos
        if (u < minU) minU = u
        if (u > maxU) maxU = u
        if (v < minV) minV = v
        if (v > maxV) maxV = v
    }
    if (minU > maxU) return null
    val cu = (minU + maxU) / 2
    val cv = (minV + maxV) / 2
    return TiltedBox((cu * cos - cv * sin).toFloat(), (cu * sin + cv * cos).toFloat(), (maxU - minU + 1).toFloat(), (maxV - minV + 1).toFloat())
}
