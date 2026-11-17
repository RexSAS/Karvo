package com.karvo.data

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** The relay turns a report into a public GitHub issue on RexSAS/Karvo (karvo-web relay/src/index.js, POST /report). */
private const val REPORT_URL = "https://link.rxsas.it/report"

/** The relay ends the reporter's comments with this line (same GitHub author as the developer's, so this tells them apart). */
private const val REPORTER_MARK = "_Reply from the reporter in Karvo._"

/** The relay's limit (10 an hour per connection, 100 a day) was reached. */
class TooManyReportsException : Exception("Too many reports")

/** [key] lets this phone comment on its own report; null for reports sent before comments existed. */
data class SentReport(val number: Int, val title: String, val key: String? = null)

/** Sends what the user typed plus app version and phone model; throws if it didn't go through. */
suspend fun sendReport(title: String, text: String, app: String, device: String): SentReport {
    val body = JSONObject().put("title", title).put("text", text).put("app", app).put("device", device)
    val reply = JSONObject(postRelay(REPORT_URL, body))
    return SentReport(reply.getInt("number"), title, reply.optString("key").ifEmpty { null })
}

/** Adds a comment to this phone's own report. */
suspend fun sendComment(report: SentReport, text: String) {
    postRelay("$REPORT_URL/${report.number}/comment", JSONObject().put("key", report.key).put("text", text))
}

private suspend fun postRelay(url: String, body: JSONObject): String = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = connection.responseCode
        if (code == 429) throw TooManyReportsException()
        if (code !in 200..299) error("Relay: HTTP $code")
        if (code == HttpURLConnection.HTTP_NO_CONTENT) "" else connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

/** Reports sent from this phone, newest first (number, title and reply key; the rest is on GitHub). */
fun sentReports(context: Context): List<SentReport> =
    context.getSharedPreferences("reports", Context.MODE_PRIVATE).getString("sent", null)?.let { json ->
        JSONArray(json).let { a ->
            (0 until a.length()).map { a.getJSONObject(it).let { o -> SentReport(o.getInt("n"), o.getString("t"), o.optString("k").ifEmpty { null }) } }
        }
    } ?: emptyList()

fun rememberReport(context: Context, report: SentReport) {
    val all = listOf(report) + sentReports(context)
    val json = JSONArray(all.map { r -> JSONObject().put("n", r.number).put("t", r.title).also { o -> r.key?.let { o.put("k", it) } } })
    context.getSharedPreferences("reports", Context.MODE_PRIVATE).edit().putString("sent", json.toString()).apply()
}

/** [mine]: written by this reporter from the app; otherwise an answer (RexSAS = Karvo) or someone else on GitHub. */
data class ReportReply(val author: String, val body: String, val date: String, val mine: Boolean)
data class ReportStatus(val open: Boolean, val replies: List<ReportReply>)

/** Public repo, so no token: GitHub's anonymous limit (60 requests an hour) is plenty for a few reports. */
suspend fun fetchReportStatus(number: Int): ReportStatus = withContext(Dispatchers.IO) {
    val issue = JSONObject(getGitHub("issues/$number"))
    val comments = JSONArray(getGitHub("issues/$number/comments"))
    ReportStatus(
        open = issue.getString("state") == "open",
        replies = (0 until comments.length()).map { i ->
            comments.getJSONObject(i).let {
                val body = it.getString("body").trim()
                val mine = body.endsWith(REPORTER_MARK)
                ReportReply(
                    author = it.getJSONObject("user").getString("login"),
                    body = if (mine) body.removeSuffix(REPORTER_MARK).trim() else body,
                    date = it.getString("created_at").take(10),
                    mine = mine,
                )
            }
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
