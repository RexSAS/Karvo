package com.masterski.data.update

import com.masterski.domain.AppVersion
import com.masterski.domain.parseVersion
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
    if (version == null || apk == null) {
        null
    } else {
        ReleaseInfo(
            version = version,
            title = release.optString("name"),
            notes = release.optString("body"),
            apkUrl = apk.getString("browser_download_url"),
            apkSize = apk.getLong("size"),
        )
    }
} catch (_: JSONException) {
    null
}
