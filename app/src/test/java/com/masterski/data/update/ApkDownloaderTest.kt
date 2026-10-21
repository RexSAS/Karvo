package com.masterski.data.update

import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkDownloaderTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var source: File
    private lateinit var dest: File
    private val bytes = Random(7).nextBytes(200 * 1024)

    @Before fun setUp() {
        source = tmp.newFile("source.apk").apply { writeBytes(bytes) }
        dest = File(tmp.root, "updates/MasterSki-1.3.apk")
    }

    @Test fun downloadsWholeFileWithProgress() = runTest {
        var last = 0L
        UrlApkDownloader(Dispatchers.Unconfined).download(source.toURI().toString(), bytes.size.toLong(), dest) { last = it }
        assertArrayEquals(bytes, dest.readBytes())
        assertEquals(204800L, last)
    }

    @Test fun sizeMismatchThrowsAndDeletes() {
        assertThrows(IncompleteDownloadException::class.java) {
            kotlinx.coroutines.runBlocking {
                UrlApkDownloader(Dispatchers.Unconfined).download(source.toURI().toString(), bytes.size + 1L, dest) {}
            }
        }
        assertFalse(dest.exists())
    }

    @Test fun cancellationDeletesPartialFile() = runTest {
        val started = CompletableDeferred<Unit>()
        val job = launch(Dispatchers.IO) {
            UrlApkDownloader(Dispatchers.IO).download(source.toURI().toString(), bytes.size.toLong(), dest) {
                started.complete(Unit)
                Thread.sleep(20) // keep the download running long enough to be cancelled midway
            }
        }
        started.await()
        job.cancel()
        job.join()
        assertFalse(dest.exists())
    }
}
