package com.karvo.data.update

import com.karvo.domain.AppVersion
import com.karvo.domain.parseVersion
import java.net.URI
import java.net.URISyntaxException
import org.json.JSONException
import org.json.JSONObject

/** The latest published release and the APK attached to it. */
data class ReleaseInfo(val version: AppVersion, val title: String, val notes: String, val apkUrl: String, val apkSize: Long)

interface ReleaseSource {
    /** Null on any failure, or when the latest release has no version tag or no APK. */
    suspend fun latestRelease(): ReleaseInfo?
}

/** Parses GitHub's "latest release" JSON; uses the first asset whose name ends in ".apk". */
fun parseRelease(json: String): ReleaseInfo? = try {
    val release = JSONObject(json)
    val version = parseVersion(release.optString("tag_name"))
    val assets = release.optJSONArray("assets")
    val apk = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }
        .firstOrNull { it.optString("name").endsWith(".apk") }
    val apkUrl = apk?.optString("browser_download_url")
    if (version == null || apk == null || apkUrl == null || !isGitHubHttps(apkUrl)) {
        null
    } else {
        ReleaseInfo(
            version = version,
            title = release.optString("name"),
            notes = release.optString("body"),
            apkUrl = apkUrl,
            apkSize = apk.getLong("size"),
        )
    }
} catch (_: JSONException) {
    null
}

/** Only release assets served over HTTPS by github.com are downloaded (GitHub then redirects to its CDN). */
private fun isGitHubHttps(url: String): Boolean = try {
    val uri = URI(url)
    uri.scheme == "https" && uri.host == "github.com"
} catch (_: URISyntaxException) {
    false
}
