package com.karvo.data.moments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.karvo.R
import java.io.File

/** The four Karvo designs a photo can be shared in, so every shared moment carries the app's name. */
enum class ShareStyle { Banner, Polaroid, Story, Stamp }

private const val NIGHT = 0xFF0A1229.toInt()
private const val SKY_TOP = 0xFF060A17.toInt()
private const val SKY_BOTTOM = 0xFF0C1530.toInt()
private const val GOLD = 0xFFF2C879.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val PAPER = 0xFFF7F4EE.toInt()
private const val INK_SOFT = 0xFF5B6478.toInt()
private const val EMBLEM_ASPECT = 72f / 56f

/** [photo] redrawn in [style], with the Karvo emblem and name, [date] and [tagline]. */
fun renderShareCard(context: Context, photo: Bitmap, style: ShareStyle, date: String, tagline: String): Bitmap {
    // Decoded photos live in GPU memory, which a software Canvas can't draw from.
    val source = if (photo.config == Bitmap.Config.HARDWARE) photo.copy(Bitmap.Config.ARGB_8888, false) else photo
    return when (style) {
        ShareStyle.Banner -> banner(context, source, date)
        ShareStyle.Polaroid -> polaroid(context, source, date, tagline)
        ShareStyle.Story -> story(context, source, date, tagline)
        ShareStyle.Stamp -> stamp(context, source, date)
    }
}

/** Writes [card] to the app's cache (replacing the last one) and returns a uri other apps can read. */
fun saveShareCard(context: Context, card: Bitmap): Uri {
    val file = File(File(context.cacheDir, "shared").apply { mkdirs() }, "karvo-moment.jpg")
    file.outputStream().use { card.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.moments", file)
}

/** The full photo with a dark fade along the bottom carrying the emblem, KARVO and the date. */
private fun banner(context: Context, photo: Bitmap, date: String): Bitmap {
    val w = photo.width.toFloat()
    val h = photo.height.toFloat()
    val u = minOf(w, h) / 100f
    return draw(photo.width, photo.height) {
        drawBitmap(photo, 0f, 0f, null)
        val fade = Paint().apply { shader = LinearGradient(0f, h * 0.72f, 0f, h, 0x00000000, 0xC0000000.toInt(), Shader.TileMode.CLAMP) }
        drawRect(0f, h * 0.72f, w, h, fade)
        val base = h - 6 * u
        emblem(context, WHITE, 6 * u, base - 6 * u, 7 * u)
        val name = text(5.5f * u, WHITE, bold = true, spacing = 0.25f)
        drawText("KARVO", 6 * u + 7 * u * EMBLEM_ASPECT + 2.5f * u, base - 0.6f * u, name)
        drawText(date, w - 6 * u, base - 0.6f * u, text(3.4f * u, 0xD9FFFFFF.toInt()).apply { textAlign = Paint.Align.RIGHT })
    }
}

/** An instant-photo print: paper border, wider at the bottom, where the name and date are written. */
private fun polaroid(context: Context, photo: Bitmap, date: String, tagline: String): Bitmap {
    val u = minOf(photo.width, photo.height) / 100f
    val side = 5 * u
    val bottom = 24 * u
    val w = photo.width + 2 * side
    val h = photo.height + side + bottom
    return draw(w.toInt(), h.toInt()) {
        drawColor(PAPER)
        drawBitmap(photo, side, side, null)
        val mid = photo.height + side + bottom / 2
        emblem(context, NIGHT, side, mid - 4.5f * u, 9 * u)
        val textX = side + 9 * u * EMBLEM_ASPECT + 3 * u
        drawText("Karvo", textX, mid + 1.5f * u, text(7 * u, NIGHT, bold = true))
        drawText(tagline, textX, mid + 6 * u, text(3.2f * u, INK_SOFT))
        drawText(date, w - side, mid + 1.5f * u, text(3.6f * u, INK_SOFT).apply { textAlign = Paint.Align.RIGHT })
    }
}

/** A 9:16 story card: night-sky backdrop, the photo with rounded corners in the middle, gold Karvo branding. */
private fun story(context: Context, photo: Bitmap, date: String, tagline: String): Bitmap {
    val w = 1080f
    val h = 1920f
    return draw(w.toInt(), h.toInt()) {
        drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, SKY_TOP, SKY_BOTTOM, Shader.TileMode.CLAMP) })
        val emblemH = 70f
        emblem(context, GOLD, (w - emblemH * EMBLEM_ASPECT) / 2, 150f, emblemH)
        drawText("KARVO", w / 2, 300f, text(64f, GOLD, bold = true, spacing = 0.35f).apply { textAlign = Paint.Align.CENTER })
        // Fit the photo in the middle band, keeping its shape.
        val scale = minOf(920f / photo.width, 1180f / photo.height)
        val pw = photo.width * scale
        val ph = photo.height * scale
        val rect = RectF((w - pw) / 2, 380f + (1180f - ph) / 2, (w + pw) / 2, 380f + (1180f + ph) / 2)
        save()
        clipPath(Path().apply { addRoundRect(rect, 40f, 40f, Path.Direction.CW) })
        drawBitmap(photo, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
        restore()
        drawText(date, w / 2, 1680f, text(44f, WHITE).apply { textAlign = Paint.Align.CENTER })
        drawText(tagline, w / 2, 1750f, text(36f, GOLD).apply { textAlign = Paint.Align.CENTER })
    }
}

/** The full photo with a round gold-ringed Karvo stamp in the top corner and the date under it. */
private fun stamp(context: Context, photo: Bitmap, date: String): Bitmap {
    val w = photo.width.toFloat()
    val u = minOf(w, photo.height.toFloat()) / 100f
    val r = 11 * u
    val cx = w - 5 * u - r
    val cy = 5 * u + r
    return draw(photo.width, photo.height) {
        drawBitmap(photo, 0f, 0f, null)
        drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = NIGHT; alpha = 210 })
        drawCircle(cx, cy, r - 1.2f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.6f * u; color = GOLD })
        val emblemH = 6 * u
        emblem(context, GOLD, cx - emblemH * EMBLEM_ASPECT / 2, cy - 6 * u, emblemH)
        drawText("KARVO", cx, cy + 4.5f * u, text(3.2f * u, GOLD, bold = true, spacing = 0.25f).apply { textAlign = Paint.Align.CENTER })
        drawText(date, cx, cy + r + 4.5f * u, text(3 * u, WHITE).apply {
            textAlign = Paint.Align.CENTER
            setShadowLayer(0.6f * u, 0f, 0f, 0x99000000.toInt())
        })
    }
}

private fun draw(width: Int, height: Int, block: Canvas.() -> Unit): Bitmap =
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { Canvas(it).block() }

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
