package com.karvo.link

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.atomic.AtomicBoolean

interface RelaySocket {
    fun send(bytes: ByteArray): Boolean
    fun close(code: Int)
}

interface RelaySocketListener {
    fun onOpen()
    fun onMessage(bytes: ByteArray)
    /** [httpStatus] is set when the relay refused the connection (404 = room gone, 401 = wrong key). */
    fun onClosed(code: Int, opened: Boolean, httpStatus: Int?)
}

interface RelaySocketFactory {
    fun open(url: String, listener: RelaySocketListener): RelaySocket
    /** HTTP DELETE on the room (unlink); returns the status. */
    suspend fun delete(url: String): Int
}

class OkHttpRelaySocketFactory(private val client: OkHttpClient) : RelaySocketFactory {
    override fun open(url: String, listener: RelaySocketListener): RelaySocket {
        val opened = AtomicBoolean(false)
        val reported = AtomicBoolean(false)
        val ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { opened.set(true); listener.onOpen() }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = listener.onMessage(bytes.toByteArray())
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(1000, null) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (reported.compareAndSet(false, true)) listener.onClosed(code, opened.get(), null)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (reported.compareAndSet(false, true)) listener.onClosed(1006, opened.get(), response?.code)
            }
        })
        return object : RelaySocket {
            override fun send(bytes: ByteArray) = ws.send(bytes.toByteString())
            override fun close(code: Int) { ws.close(code, null) }
        }
    }

    override suspend fun delete(url: String): Int = withContext(Dispatchers.IO) {
        runCatching { client.newCall(Request.Builder().url(url).delete().build()).execute().use { it.code } }.getOrDefault(-1)
    }
}
