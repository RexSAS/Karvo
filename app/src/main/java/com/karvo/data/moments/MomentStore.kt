package com.karvo.data.moments

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.OutputOptions
import androidx.core.content.FileProvider
import com.karvo.domain.MAX_VIDEO_MILLIS
import com.karvo.domain.Moment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Instant

private const val ALBUM = "Karvo"
private const val JPEG_QUALITY = 95

/**
 * Moments live in the shared gallery (Pictures/Karvo and Movies/Karvo) so Google Photos can back them up and they
 * survive uninstalling. Android 10+ goes through MediaStore; Android 8–9 write plain files and scan them in.
 */
class MomentStore(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val scoped = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun legacyDir(type: String) = File(Environment.getExternalStoragePublicDirectory(type), ALBUM).apply { mkdirs() }

    private fun name(ext: String) = "karvo-${System.currentTimeMillis()}.$ext"

    /** Saves [bitmap] as a high-quality JPEG in the Karvo album and returns its uri. */
    suspend fun savePhoto(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        if (scoped) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name("jpg"))
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
                // Hidden from the gallery until it's fully written.
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: throw IOException("Can't create the photo")
            try {
                resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                    ?: throw IOException("Can't write the photo")
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            uri
        } else {
            val file = File(legacyDir(Environment.DIRECTORY_PICTURES), name("jpg"))
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            Uri.fromFile(file).also(::scan)
        }
    }

    fun videoOutput(): OutputOptions = if (scoped) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name("mp4"))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$ALBUM")
        }
        MediaStoreOutputOptions.Builder(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values).setDurationLimitMillis(MAX_VIDEO_MILLIS).build()
    } else {
        FileOutputOptions.Builder(File(legacyDir(Environment.DIRECTORY_MOVIES), name("mp4")))
            .setDurationLimitMillis(MAX_VIDEO_MILLIS).build()
    }

    /** Android 8–9: makes a freshly written file show up in the gallery. */
    fun scan(uri: Uri) {
        if (!scoped && uri.scheme == "file") MediaScannerConnection.scanFile(appContext, arrayOf(uri.path), null, null)
    }

    suspend fun list(): List<Moment> = withContext(Dispatchers.IO) {
        if (scoped) query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false) + query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
        else listFiles(Environment.DIRECTORY_PICTURES, false) + listFiles(Environment.DIRECTORY_MOVIES, true)
    }

    suspend fun delete(moment: Moment): Boolean = withContext(Dispatchers.IO) {
        if (moment.uri.scheme == "file") File(moment.uri.path!!).delete().also { scan(moment.uri) }
        else resolver.delete(moment.uri, null, null) > 0
    }

    /** A uri other apps can read: gallery uris as they are, Android 8–9 files through the FileProvider. */
    fun shareUri(moment: Moment): Uri =
        if (moment.uri.scheme == "file") FileProvider.getUriForFile(appContext, "${appContext.packageName}.moments", File(moment.uri.path!!))
        else moment.uri

    // Only files this install created are visible without a gallery permission, which is all Karvo needs.
    private fun query(collection: Uri, isVideo: Boolean): List<Moment> {
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_ADDED)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val dir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        return resolver.query(collection, projection, selection, arrayOf("$dir/$ALBUM/%"), null)?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val taken = c.getLong(1).takeIf { it > 0 } ?: (c.getLong(2) * 1000)
                    add(Moment(Uri.withAppendedPath(collection, c.getLong(0).toString()), isVideo, Instant.ofEpochMilli(taken)))
                }
            }
        }.orEmpty()
    }

    private fun listFiles(type: String, isVideo: Boolean): List<Moment> =
        legacyDir(type).listFiles().orEmpty()
            .filter { it.isFile && it.length() > 0 && it.name.startsWith("karvo-") }
            .map { Moment(Uri.fromFile(it), isVideo, Instant.ofEpochMilli(it.lastModified())) }
}
