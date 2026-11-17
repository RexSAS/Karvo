package com.karvo.data

import android.content.Context
import java.net.HttpURLConnection
import org.json.JSONArray
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** The relay turns a report into a public GitHub issue on RexSAS/Karvo (karvo-web relay/src/index.js, POST /report). */
private const val REPORT_URL = "https://link.rxsas.it/report"

/** Sends what the user typed plus app version and phone model. Returns the issue number; throws if it didn't go through. */
suspend fun sendReport(title: String, text: String, app: String, device: String): Int = withContext(Dispatchers.IO) {
    val connection = URL(REPORT_URL).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        val body = JSONObject().put("title", title).put("text", text).put("app", app).put("device", device).toString()
        connection.outputStream.use { it.write(body.toByteArray()) }
        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) error("Report failed: HTTP $code")
        JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getInt("number")
    } finally {
        connection.disconnect()
    }
}

data class SentReport(val number: Int, val title: String)

/** Reports sent from this phone, newest first (only number and title; the rest is on GitHub). */
fun sentReports(context: Context): List<SentReport> =
    context.getSharedPreferences("reports", Context.MODE_PRIVATE).getString("sent", null)?.let { json ->
        JSONArray(json).let { a -> (0 until a.length()).map { a.getJSONObject(it).let { o -> SentReport(o.getInt("n"), o.getString("t")) } } }
    } ?: emptyList()

fun rememberReport(context: Context, report: SentReport) {
    val all = listOf(report) + sentReports(context)
    val json = JSONArray(all.map { JSONObject().put("n", it.number).put("t", it.title) })
    context.getSharedPreferences("reports", Context.MODE_PRIVATE).edit().putString("sent", json.toString()).apply()
}

data class ReportReply(val author: String, val body: String, val date: String)
data class ReportStatus(val open: Boolean, val replies: List<ReportReply>)

/** Public repo, so no token: GitHub's anonymous limit (60 requests an hour) is plenty for a few reports. */
suspend fun fetchReportStatus(number: Int): ReportStatus = withContext(Dispatchers.IO) {
    val issue = JSONObject(getGitHub("issues/$number"))
    val comments = JSONArray(getGitHub("issues/$number/comments"))
    ReportStatus(
        open = issue.getString("state") == "open",
        replies = (0 until comments.length()).map { i ->
            comments.getJSONObject(i).let { ReportReply(it.getJSONObject("user").getString("login"), it.getString("body"), it.getString("created_at").take(10)) }
        },
    )
}

private fun getGitHub(path: String): String {
    val connection = URL("https://api.github.com/repos/RexSAS/Karvo/$path").openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Karvo")
        if (connection.responseCode != HttpURLConnection.HTTP_OK) error("GitHub: HTTP ${connection.responseCode}")
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
