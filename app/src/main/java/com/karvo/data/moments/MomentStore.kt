package com.karvo.data.moments

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.OutputOptions
import androidx.core.content.FileProvider
import com.karvo.domain.MAX_VIDEO_MILLIS
import com.karvo.domain.Moment
import com.karvo.domain.karvoNameMillis
import com.karvo.domain.momentsThatFit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId

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
        val now = System.currentTimeMillis()
        write(isVideo = false, "image/jpeg", "karvo-$now.jpg", now) { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    }

    /**
     * Brings back the Karvo moments in [folders] (folder-access grants for Pictures/Karvo and Movies/Karvo): after a
     * reinstall the album still holds them, but they're no longer this install's, so Karvo can't see them. Each one is
     * copied back as this install's and the old file is deleted. Returns how many came back and how many were skipped
     * (already back, or their day is full).
     */
    suspend fun restoreFrom(folders: List<Uri>, zone: ZoneId): Pair<Int, Int> {
        val found = withContext(Dispatchers.IO) { folders.flatMap(::karvoFilesIn) }
        var restored = 0
        for (moment in momentsThatFit(list(), found, zone)) {
            try {
                restore(moment)
                restored++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Left where it is; counted as skipped.
            }
        }
        return restored to found.size - restored
    }

    private suspend fun restore(old: Moment) = withContext(Dispatchers.IO) {
        val mime = resolver.getType(old.uri) ?: throw IOException("Unknown file type")
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: if (old.isVideo) "mp4" else "jpg"
        val input = resolver.openInputStream(old.uri) ?: throw IOException("Can't read ${old.uri}")
        input.use { source -> write(old.isVideo, mime, "karvo-${old.takenAt.toEpochMilli()}.$ext", old.takenAt.toEpochMilli()) { source.copyTo(it) } }
        try {
            DocumentsContract.deleteDocument(resolver, old.uri)
        } catch (_: Exception) {
            // The copy is safe; a leftover original only shows twice in the gallery.
        }
    }

    /** The photos and videos Karvo named, directly inside the granted folder [tree]. */
    private fun karvoFilesIn(tree: Uri): List<Moment> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        return resolver.query(children, columns, null, null, null)?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val millis = karvoNameMillis(c.getString(1) ?: continue) ?: continue
                    val mime = c.getString(2) ?: continue
                    if (!mime.startsWith("image/") && !mime.startsWith("video/")) continue
                    add(Moment(DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)), mime.startsWith("video/"), Instant.ofEpochMilli(millis)))
                }
            }
        }.orEmpty()
    }

    private fun write(isVideo: Boolean, mime: String, fileName: String, takenMillis: Long, body: (OutputStream) -> Unit): Uri {
        val dir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        if (!scoped) {
            val file = File(legacyDir(dir), fileName)
            try {
                file.outputStream().use(body)
            } catch (e: Exception) {
                file.delete()
                throw e
            }
            // Android 8–9 moments are dated by the file itself.
            file.setLastModified(takenMillis)
            return Uri.fromFile(file).also(::scan)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/$ALBUM")
            put(MediaStore.MediaColumns.DATE_TAKEN, takenMillis)
            // Hidden from the gallery until it's fully written.
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: throw IOException("Can't create the file")
        try {
            resolver.openOutputStream(uri)?.use(body) ?: throw IOException("Can't write the file")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    private fun queryDateTaken(uri: Uri): Long? = try {
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATE_TAKEN), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0).takeIf { it > 0 } else null
        }
    } catch (_: RuntimeException) {
        null // providers that don't know the column
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
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.DISPLAY_NAME)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val dir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        return resolver.query(collection, projection, selection, arrayOf("$dir/$ALBUM/%"), null)?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    // The name keeps the exact capture time, even after a restore; the gallery's dates can drift.
                    val taken = c.getString(3)?.let(::karvoNameMillis) ?: c.getLong(1).takeIf { it > 0 } ?: (c.getLong(2) * 1000)
                    add(Moment(Uri.withAppendedPath(collection, c.getLong(0).toString()), isVideo, Instant.ofEpochMilli(taken)))
                }
            }
        }.orEmpty()
    }

    private fun listFiles(type: String, isVideo: Boolean): List<Moment> =
        legacyDir(type).listFiles().orEmpty()
            .filter { it.isFile && it.length() > 0 && it.name.startsWith("karvo-") }
            .map { Moment(Uri.fromFile(it), isVideo, Instant.ofEpochMilli(karvoNameMillis(it.name) ?: it.lastModified())) }
}
