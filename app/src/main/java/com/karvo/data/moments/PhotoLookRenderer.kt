package com.karvo.data.moments

import android.graphics.Bitmap
import android.graphics.Matrix
import com.karvo.domain.PhotoLook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val BAND_ROWS = 128

/** [bitmap] turned upright by [degrees], as a mutable copy ready for [applyPhotoLook]. */
fun uprightCopy(bitmap: Bitmap, degrees: Int): Bitmap =
    if (degrees == 0) bitmap.copy(Bitmap.Config.ARGB_8888, true)
    else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
        .let { if (it.isMutable) it else it.copy(Bitmap.Config.ARGB_8888, true) }

/** Applies [PhotoLook] to a mutable [bitmap] in place, in row bands spread over the CPU cores. */
suspend fun applyPhotoLook(bitmap: Bitmap) = coroutineScope {
    val width = bitmap.width
    (0 until bitmap.height step BAND_ROWS).map { top ->
        async(Dispatchers.Default) {
            val rows = minOf(BAND_ROWS, bitmap.height - top)
            val pixels = IntArray(width * rows)
            bitmap.getPixels(pixels, 0, width, 0, top, width, rows)
            PhotoLook.apply(pixels)
            bitmap.setPixels(pixels, 0, width, 0, top, width, rows)
        }
    }.awaitAll()
}
