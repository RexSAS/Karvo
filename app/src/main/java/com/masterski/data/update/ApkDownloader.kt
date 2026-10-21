package com.masterski.data.update

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class IncompleteDownloadException(val expected: Long, val actual: Long) : IOException("Downloaded $actual of $expected bytes")

interface ApkDownloader {
    /** Downloads [url] to [dest], reporting bytes read; [dest] is deleted on any failure or cancellation. */
    suspend fun download(url: String, expectedSize: Long, dest: File, onProgress: (Long) -> Unit)
}

class UrlApkDownloader(private val io: CoroutineDispatcher = Dispatchers.IO) : ApkDownloader {
    override suspend fun download(url: String, expectedSize: Long, dest: File, onProgress: (Long) -> Unit) = withContext(io) {
        dest.parentFile?.mkdirs()
        try {
            val connection = URL(url).openConnection().apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                (this as? HttpURLConnection)?.instanceFollowRedirects = true
            }
            var total = 0L
            connection.getInputStream().use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        total += read
                        onProgress(total)
                    }
                }
            }
            if (total != expectedSize) throw IncompleteDownloadException(expectedSize, total)
        } catch (t: Throwable) {
            dest.delete()
            throw t
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}
