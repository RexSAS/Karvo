package com.karvo.data.moments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.karvo.R
import java.io.File
import kotlin.math.roundToInt

/**
 * The Karvo designs a photo can be shared in, so every shared moment carries the app's name, in the picker's order:
 * [Frame] is the designer's PNG (res/drawable-nodpi/share_frame.png), 9:16 and first because it's story-sized;
 * [Banner] is drawn in code and fits any photo.
 */
enum class ShareStyle { Frame, Banner }

private const val NIGHT = 0xFF0A1229.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val BLACK = 0xFF000000.toInt()
/** Width of the black line around the photo in the Frame design, in template pixels. */
private const val PHOTO_EDGE = 4f
private const val EMBLEM_ASPECT = 72f / 56f
/** Long side of the small copy of the photo that is blurred, then stretched back up, for the background. */
private const val BLUR_PX = 96
private const val BLUR_RADIUS = 3

/** Size of an Instagram story. */
private const val STORY_WIDTH = 1080
private const val STORY_HEIGHT = 1920

/**
 * [photo] redrawn in [style], with the Karvo branding and [date]. For a [story], Banner first crops the photo to a
 * full 9:16 story (Frame is always one).
 */
fun renderShareCard(context: Context, photo: Bitmap, style: ShareStyle, date: String, story: Boolean = false): Bitmap {
    // Decoded photos live in GPU memory, which a software Canvas can't draw from.
    val source = if (photo.config == Bitmap.Config.HARDWARE) photo.copy(Bitmap.Config.ARGB_8888, false) else photo
    return when (style) {
        ShareStyle.Frame -> frame(context, source, date)
        ShareStyle.Banner -> if (story) banner(context, storySized(source), date, story = true) else banner(context, source, date)
    }
}

/** [photo] cropped from the centre to fill a story. */
private fun storySized(photo: Bitmap): Bitmap = draw(STORY_WIDTH, STORY_HEIGHT) {
    drawBitmap(photo, null, cover(photo, RectF(0f, 0f, STORY_WIDTH.toFloat(), STORY_HEIGHT.toFloat())), Paint(Paint.FILTER_BITMAP_FLAG))
}

/** Writes [card] to the app's cache (replacing the last one) and returns a uri other apps can read. */
fun saveShareCard(context: Context, card: Bitmap): Uri {
    val file = File(File(context.cacheDir, "shared").apply { mkdirs() }, "karvo-moment.jpg")
    file.outputStream().use { card.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.moments", file)
}

/**
 * The full photo with a dark fade along the bottom carrying the emblem, KARVO and the date. In a [story] the line sits
 * higher, clear of Instagram's reply bar.
 */
private fun banner(context: Context, photo: Bitmap, date: String, story: Boolean = false): Bitmap {
    val w = photo.width.toFloat()
    val h = photo.height.toFloat()
    val u = minOf(w, h) / 100f
    return draw(photo.width, photo.height) {
        drawBitmap(photo, 0f, 0f, null)
        val fadeTop = if (story) h * 0.6f else h * 0.72f
        val fade = Paint().apply { shader = LinearGradient(0f, fadeTop, 0f, h, 0x00000000, 0xC0000000.toInt(), Shader.TileMode.CLAMP) }
        drawRect(0f, fadeTop, w, h, fade)
        val base = if (story) h * 0.84f else h - 6 * u
        emblem(context, WHITE, 6 * u, base - 6 * u, 7 * u)
        val name = text(5.5f * u, WHITE, bold = true, spacing = 0.25f)
        outlinedText("KARVO", 6 * u + 7 * u * EMBLEM_ASPECT + 2.5f * u, base - 0.6f * u, name, 0.35f * u)
        outlinedText(date, w - 6 * u, base - 0.6f * u, text(3.4f * u, 0xD9FFFFFF.toInt()).apply { textAlign = Paint.Align.RIGHT }, 0.3f * u)
    }
}

/** The designer's tilted frame over a blurred copy of the photo, with the photo itself sharp inside the frame. */
private fun frame(context: Context, photo: Bitmap, date: String): Bitmap {
    val template = frameTemplate(context)
    val w = template.width.toFloat()
    val h = template.height.toFloat()
    val smooth = Paint(Paint.FILTER_BITMAP_FLAG)
    return draw(template.width, template.height) {
        // Blurred background: a small copy of the photo, blurred, stretched back up with smoothing, and dimmed.
        val scale = BLUR_PX.toFloat() / maxOf(photo.width, photo.height)
        val small = Bitmap.createScaledBitmap(photo, (photo.width * scale).roundToInt().coerceAtLeast(1), (photo.height * scale).roundToInt().coerceAtLeast(1), true)
        val soft = blurred(small, BLUR_RADIUS)
        drawBitmap(soft, null, cover(soft, RectF(0f, 0f, w, h)), smooth)
        drawColor(0x40000000)
        // The sharp photo, turned with the frame and filling its window.
        val window = template.window
        val rect = RectF(window.cx - window.width / 2, window.cy - window.height / 2, window.cx + window.width / 2, window.cy + window.height / 2)
        save()
        rotate(template.tilt, window.cx, window.cy)
        clipRect(rect)
        drawBitmap(photo, null, cover(photo, rect), smooth)
        restore()
        drawBitmap(template.image, 0f, 0f, null)
        // A thin black line where the photo meets the paper.
        save()
        rotate(template.tilt, window.cx, window.cy)
        drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = PHOTO_EDGE; color = BLACK })
        restore()
        template.date?.let { box ->
            save()
            rotate(template.tilt, box.cx, box.cy)
            val paint = text(box.height * 0.8f, NIGHT).apply { textAlign = Paint.Align.CENTER }
            // A long date shrinks to fit the box.
            paint.measureText(date).let { if (it > box.width) paint.textSize *= box.width / it }
            drawText(date, box.cx, box.cy - (paint.ascent() + paint.descent()) / 2, paint)
            restore()
        }
    }
}

private class FrameTemplate(val image: Bitmap, val width: Int, val height: Int, val tilt: Float, val window: TiltedBox, val date: TiltedBox?)

@Volatile private var frameTemplate: FrameTemplate? = null

/** The frame PNG read once per process: reading its window and date spot scans every pixel. */
private fun frameTemplate(context: Context): FrameTemplate = frameTemplate ?: run {
    val png = BitmapFactory.decodeResource(context.resources, R.drawable.share_frame, BitmapFactory.Options().apply { inScaled = false })
    val pixels = IntArray(png.width * png.height).also { png.getPixels(it, 0, png.width, 0, 0, png.width, png.height) }
    val t = readShareTemplate(pixels, png.width, png.height)
    FrameTemplate(Bitmap.createBitmap(t.pixels, t.width, t.height, Bitmap.Config.ARGB_8888), t.width, t.height, t.tilt, t.window, t.date)
}.also { frameTemplate = it }

/** Three box blurs of [radius] across and down, close to a Gaussian blur; meant for small bitmaps. */
private fun blurred(bitmap: Bitmap, radius: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    var pixels = IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }
    repeat(3) {
        pixels = boxBlur(pixels, w, h, radius, across = true)
        pixels = boxBlur(pixels, w, h, radius, across = false)
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}

/** Each pixel becomes the average of the 2×[radius]+1 around it along one direction (edges repeat). */
private fun boxBlur(src: IntArray, w: Int, h: Int, radius: Int, across: Boolean): IntArray {
    val out = IntArray(src.size)
    val lines = if (across) h else w
    val length = if (across) w else h
    val n = 2 * radius + 1
    for (line in 0 until lines) {
        fun index(k: Int): Int = k.coerceIn(0, length - 1).let { if (across) line * w + it else it * w + line }
        var r = 0
        var g = 0
        var b = 0
        for (k in -radius..radius) src[index(k)].let { r += it shr 16 and 0xFF; g += it shr 8 and 0xFF; b += it and 0xFF }
        for (k in 0 until length) {
            out[index(k)] = (0xFF shl 24) or (r / n shl 16) or (g / n shl 8) or (b / n)
            val add = src[index(k + radius + 1)]
            val drop = src[index(k - radius)]
            r += (add shr 16 and 0xFF) - (drop shr 16 and 0xFF)
            g += (add shr 8 and 0xFF) - (drop shr 8 and 0xFF)
            b += (add and 0xFF) - (drop and 0xFF)
        }
    }
    return out
}

/** Where to draw [bitmap] so it covers [target] completely, centred and keeping its shape. */
private fun cover(bitmap: Bitmap, target: RectF): RectF {
    val scale = maxOf(target.width() / bitmap.width, target.height() / bitmap.height)
    val w = bitmap.width * scale / 2
    val h = bitmap.height * scale / 2
    return RectF(target.centerX() - w, target.centerY() - h, target.centerX() + w, target.centerY() + h)
}

private fun draw(width: Int, height: Int, block: Canvas.() -> Unit): Bitmap =
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { Canvas(it).block() }

/** [text] drawn with [paint], inside a black outline [outline] pixels wide, so it reads on any photo. */
private fun Canvas.outlinedText(text: String, x: Float, y: Float, paint: Paint, outline: Float) {
    val stroke = Paint(paint).apply {
        style = Paint.Style.STROKE
        strokeWidth = outline * 2 // half of it is hidden under the letters
        strokeJoin = Paint.Join.ROUND
        color = BLACK
    }
    drawText(text, x, y, stroke)
    drawText(text, x, y, paint)
}

private fun text(size: Float, color: Int, bold: Boolean = false, spacing: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = size
    this.color = color
    letterSpacing = spacing
    if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

/** The Karvo emblem in [color], [height] tall, with its top-left corner at [x], [y]. */
private fun Canvas.emblem(context: Context, color: Int, x: Float, y: Float, height: Float) {
    val emblem = ContextCompat.getDrawable(context, R.drawable.ic_emblem)!!.mutate()
    emblem.setTint(color)
    emblem.setBounds(x.toInt(), y.toInt(), (x + height * EMBLEM_ASPECT).toInt(), (y + height).toInt())
    emblem.draw(this)
}
