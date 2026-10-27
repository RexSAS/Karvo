package com.karvo.domain

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Karvo's photo look, applied to every captured photo: lifted shadows, a soft highlight roll-off for snow and sky,
 * a gentle midtone contrast, punchy saturation for sport photos plus vibrance that gives muted colours extra
 * (sparing skin), and a touch of warmth in the midtones.
 * The tone curve works on brightness and scales the channels together, so hues and saturation survive it.
 * Works on packed ARGB ints, so it has no Android dependency and is unit tested.
 */
object PhotoLook {
    private const val SHADOW_LIFT = 0.10
    private const val HIGHLIGHT_ROLLOFF = 0.10
    private const val CONTRAST = 0.10
    /** Applied to every colour, strong ones included: sport photos look best punchy. */
    private const val SATURATION = 0.15
    /** On top of [SATURATION], only for muted colours. */
    private const val VIBRANCE = 0.25
    /** Warm oranges (skin, sunsets) get only this share of the vibrance boost. */
    private const val SKIN_VIBRANCE = 0.4
    /**
     * Levels added to red and taken from blue at mid-grey. Zero at black and white, so snow stays white, and only on
     * near-neutral tones (below [WARMTH_SATURATION]): warming a blue sky would just make it duller.
     */
    private const val WARMTH = 3.0
    private const val WARMTH_SATURATION = 0.15

    /** Brightness curve, 0–1 in and out: endpoints fixed, monotonic. */
    internal fun tone(x: Double): Double {
        var y = x + SHADOW_LIFT * x * (1 - x).pow(3) * 4 // opens shadows, fades out by the midtones
        y -= HIGHLIGHT_ROLLOFF * x.pow(3) * (1 - x) * 4 // eases highlights before white
        y += CONTRAST * (x - 0.5) * x * (1 - x) * 2 // a soft S around the midtones
        return y.coerceIn(0.0, 1.0)
    }

    // One gain per brightness level (0–255), so the per-pixel work is a table lookup.
    private val gain = DoubleArray(256) { level -> if (level == 0) 1.0 else tone(level / 255.0) * 255 / level }

    private fun luma(r: Double, g: Double, b: Double) = (r * 299 + g * 587 + b * 114) / 1000

    fun apply(argb: Int): Int {
        val k = gain[luma(((argb shr 16) and 0xFF).toDouble(), ((argb shr 8) and 0xFF).toDouble(), (argb and 0xFF).toDouble()).roundToInt()]
        var r = ((argb shr 16) and 0xFF) * k
        var g = ((argb shr 8) and 0xFF) * k
        var b = (argb and 0xFF) * k

        // Saturation and vibrance: push each channel away from the grey of the same brightness, more for muted colours.
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val saturation = if (max <= 0) 0.0 else (max - min) / max
        var vibrance = VIBRANCE * (1 - saturation)
        if (r > g && g > b) vibrance *= SKIN_VIBRANCE
        val boost = SATURATION + vibrance
        val grey = luma(r, g, b)
        val level = (grey / 255).coerceIn(0.0, 1.0)
        val warmth = WARMTH * level * (1 - level) * 4 * (1 - saturation / WARMTH_SATURATION).coerceAtLeast(0.0)
        r += (r - grey) * boost + warmth
        g += (g - grey) * boost
        b += (b - grey) * boost - warmth

        fun channel(v: Double) = v.roundToInt().coerceIn(0, 255)
        return (argb and 0xFF000000.toInt()) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }

    /** Applies the look to [pixels] in place. */
    fun apply(pixels: IntArray) {
        for (i in pixels.indices) pixels[i] = apply(pixels[i])
    }
}
