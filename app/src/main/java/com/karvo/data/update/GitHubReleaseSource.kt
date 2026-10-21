package com.karvo.data.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LATEST_RELEASE = "https://api.github.com/repos/RexSAS/Karvo/releases/latest"

/** Reads the latest release from GitHub. Sends no user data: only Accept and User-Agent headers. */
class GitHubReleaseSource(
    private val currentVersion: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ReleaseSource {
    override suspend fun latestRelease(): ReleaseInfo? = withContext(io) {
        try {
            val connection = URL(LATEST_RELEASE).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "Karvo/$currentVersion")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                parseRelease(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
