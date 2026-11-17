package com.karvo.data

import java.net.HttpURLConnection
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
