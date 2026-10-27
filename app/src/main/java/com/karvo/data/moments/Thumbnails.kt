package com.karvo.data.moments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.karvo.domain.Moment

/**
 * A bitmap of [moment] no larger than [maxPx] on its long side, upright; null if it can't be read.
 * Blocking: call off the main thread.
 */
fun loadMomentBitmap(context: Context, moment: Moment, maxPx: Int): Bitmap? = try {
    val uri = moment.uri
    if (uri.scheme == "file") {
        val path = uri.path!!
        @Suppress("DEPRECATION")
        if (moment.isVideo) ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND)
        else decodeLegacyPhoto(path, maxPx)
    } else if (maxPx <= THUMBNAIL_MAX_PX) {
        // Small tiles: the gallery's cached thumbnail is fast and sharp enough. Content uris only exist on Android 10+.
        context.contentResolver.loadThumbnail(uri, Size(maxPx, maxPx), null)
    } else if (moment.isVideo) {
        videoFrame(context, moment, maxPx) ?: context.contentResolver.loadThumbnail(uri, Size(maxPx, maxPx), null)
    } else {
        // Large views decode the photo itself: gallery thumbnails are too soft full screen.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.setTargetSampleSize(sampleSize(maxOf(info.size.width, info.size.height), maxPx))
        }
    }
} catch (_: Exception) {
    null
}

/** Above this, moments are decoded from the file instead of the gallery thumbnail. */
private const val THUMBNAIL_MAX_PX = 512

/** The largest power of two that keeps the long side at or above [maxPx]: uniform, so the aspect never changes. */
private fun sampleSize(longSide: Int, maxPx: Int): Int {
    var sample = 1
    while (longSide / (sample * 2) >= maxPx) sample *= 2
    return sample
}

private fun videoFrame(context: Context, moment: Moment, maxPx: Int): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, moment.uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxPx, maxPx)
        } else {
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    } finally {
        retriever.release()
    }
}

private fun decodeLegacyPhoto(path: String, maxPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val sample = sampleSize(maxOf(bounds.outWidth, bounds.outHeight), maxPx)
    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val degrees = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
}
