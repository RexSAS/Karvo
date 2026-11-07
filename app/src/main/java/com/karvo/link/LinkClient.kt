package com.karvo.link

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.time.Instant

const val RELAY_URL = "wss://link.rxsas.it"

enum class LinkState { OFFLINE, CONNECTING, CONNECTED, REPLACED, BROKEN, OUTDATED, UNLINKED }

sealed interface PairResult {
    data class Linked(val pcName: String) : PairResult
    data object Expired : PairResult
    data object Outdated : PairResult
    data object Failed : PairResult
}

/**
 * The phone's link to one PC through the relay (spec "Phone behaviour"), the mirror of karvo-web/link/connection.js:
 * hello handshake, frames handled strictly in order, commands applied by [handler], snapshots after every change,
 * pings, and reconnecting with backoff.
 */
class LinkClient(
    pc: LinkedPc,
    private val handler: LinkCommandHandler,
    private val factory: RelaySocketFactory,
    private val scope: CoroutineScope,
    private val appVersion: String,
    private val myName: String,
    relayUrl: String = RELAY_URL,
    private val now: () -> Instant = Instant::now,
) : LinkPeer {
    override val roomId: String = pc.roomId
    private val roomBytes = LinkCrypto.fromB64url(pc.roomId)
    private val keys = LinkCrypto.deriveKeys(requireNotNull(pc.secret) { "broken pairing" }, roomBytes)
    private val url = "$relayUrl/room/${pc.roomId}?role=phone&auth=${LinkCrypto.b64url(keys.authPhone)}"

    private val _state = MutableStateFlow(LinkState.OFFLINE)
    override val state: StateFlow<LinkState> = _state.asStateFlow()
    private val _lastCommandAt = MutableStateFlow<Instant?>(null)
    override val lastCommandAt: StateFlow<Instant?> = _lastCommandAt.asStateFlow()
    private val _pcName = MutableStateFlow(pc.pcName)
    override val pcName: StateFlow<String> = _pcName.asStateFlow()

    private sealed interface Event {
        data object Open : Event
        class Message(val bytes: ByteArray) : Event
        class Closed(val code: Int, val opened: Boolean, val status: Int?) : Event
    }

    private var running = false
    private var socket: RelaySocket? = null
    private var channel: LinkChannel? = null
    private var snapshotJob: Job? = null
    private var pingJob: Job? = null
    private var retryJob: Job? = null
    private val sendLock = Mutex()
    private var backoff = FIRST_BACKOFF
    private var bad = 0
    private var missed = 0
    private var pcOutdated = false

    override fun start() {
        if (running) return
        running = true; bad = 0; backoff = FIRST_BACKOFF
        connect()
    }

    private fun connect() {
        if (!running) return
        if (!pcOutdated) _state.value = LinkState.CONNECTING
        val inbox = Channel<Event>(Channel.UNLIMITED)
        val ch = LinkChannel(keys.phoneToPc, keys.pcToPhone, roomBytes, 2, 1)
        val sock = factory.open(url, object : RelaySocketListener {
            override fun onOpen() { inbox.trySend(Event.Open) }
            override fun onMessage(bytes: ByteArray) { inbox.trySend(Event.Message(bytes)) }
            override fun onClosed(code: Int, opened: Boolean, httpStatus: Int?) { inbox.trySend(Event.Closed(code, opened, httpStatus)) }
        })
        socket = sock; channel = ch
        // One consumer per socket: frames are handled one at a time, in arrival order.
        scope.launch {
            for (e in inbox) {
                if (socket !== sock) break
                when (e) {
                    Event.Open -> { if (!pcOutdated) _state.value = LinkState.OFFLINE; sendLock.withLock { sock.send(ch.hello(appVersion, myName)) } }
                    is Event.Message -> onFrame(sock, ch, e.bytes)
                    is Event.Closed -> onClosed(sock, e)
                }
            }
        }
    }

    private suspend fun onFrame(sock: RelaySocket, ch: LinkChannel, bytes: ByteArray) {
        val msg = try { ch.decode(bytes) } catch (e: ProtocolException) {
            if (e.code == "version") {
                if ((e.peerVersion ?: 0) > PROTOCOL_VERSION) return end(LinkState.OUTDATED) // only a Karvo update helps
                pcOutdated = true; _state.value = LinkState.OUTDATED // the page is old: keep trying slowly
                return dropAndRetry(sock, 4005, MAX_BACKOFF)
            }
            if (++bad >= MAX_BAD) return end(LinkState.BROKEN)
            return dropAndRetry(sock, 4003, backoff)
        }
        missed = 0
        when (msg.type) {
            "hello" -> if (msg.isNewHello) {
                sendLock.withLock { sock.send(ch.hello(appVersion, myName)) } // the PC may have missed ours
                bad = 0; backoff = FIRST_BACKOFF; pcOutdated = false
                msg.json.optString("name").takeIf { it.isNotBlank() }?.let { _pcName.value = it }
                _state.value = LinkState.CONNECTED
                snapshotJob?.cancel()
                snapshotJob = scope.launch { handler.snapshots().collect { runCatching { send("snapshot", it) } } }
                pingJob?.cancel()
                pingJob = scope.launch { pingLoop(sock) }
            }
            "command" -> {
                _lastCommandAt.value = now()
                val ack = JSONObject().put("id", msg.json.opt("id"))
                try {
                    val result = handler.handle(msg.json.optString("op"), msg.json.optJSONObject("args") ?: JSONObject())
                    ack.put("ok", true); if (result != null) ack.put("result", result)
                } catch (e: CommandRefused) { ack.put("ok", false).put("error", e.message)
                } catch (e: Exception) { ack.put("ok", false).put("error", e.message ?: "Error") }
                runCatching { send("ack", ack) }
            }
            "unpaired" -> end(LinkState.UNLINKED)
            "bye" -> { _state.value = LinkState.OFFLINE; snapshotJob?.cancel(); pingJob?.cancel() }
        }
    }

    private fun onClosed(sock: RelaySocket, e: Event.Closed) {
        if (socket !== sock) return
        detach()
        when {
            e.code == 4000 -> end(LinkState.REPLACED)
            e.code == 4002 -> end(LinkState.UNLINKED)
            !e.opened && e.status == 404 -> end(LinkState.UNLINKED)
            !e.opened && e.status == 401 -> end(LinkState.BROKEN)
            pcOutdated -> scheduleRetry(MAX_BACKOFF)
            else -> { _state.value = LinkState.OFFLINE; scheduleRetry(backoff) }
        }
    }

    private suspend fun pingLoop(sock: RelaySocket) {
        while (true) {
            delay(PING_MS)
            if (++missed > MAX_MISSED) return dropAndRetry(sock, 4004, backoff)
            runCatching { send("ping") }
        }
    }

    private fun detach() {
        socket = null; channel = null
        snapshotJob?.cancel(); pingJob?.cancel()
    }

    private fun dropAndRetry(sock: RelaySocket, code: Int, delayMs: Long) {
        if (socket !== sock) return
        detach(); sock.close(code)
        if (!pcOutdated) _state.value = LinkState.OFFLINE
        scheduleRetry(delayMs)
    }

    private fun scheduleRetry(delayMs: Long) {
        retryJob?.cancel()
        retryJob = scope.launch { delay(delayMs); connect() }
        backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
    }

    private fun end(s: LinkState) {
        running = false
        retryJob?.cancel()
        val sock = socket
        detach(); sock?.close(1000)
        _state.value = s
    }

    private suspend fun send(type: String, fields: JSONObject = JSONObject()) = sendLock.withLock {
        val ch = channel; val sock = socket
        check(_state.value == LinkState.CONNECTED && ch != null && sock != null) { "not connected" }
        sock.send(ch.encode(type, fields))
    }

    /** Disconnect (notification, idle timeout, app closed): tell the PC, then close. */
    override suspend fun stop(sendBye: Boolean) {
        if (sendBye && _state.value == LinkState.CONNECTED) runCatching { send("bye") }
        end(LinkState.OFFLINE)
    }

    /** Remove this PC: tell it, close, and delete the room on the relay. */
    override suspend fun unpair() {
        if (_state.value == LinkState.CONNECTED) runCatching { send("unpaired") }
        end(LinkState.UNLINKED)
        factory.delete(url.replaceFirst("wss://", "https://").replaceFirst("ws://", "http://"))
    }

    /** First connection right after scanning: waits for the PC's hello. */
    override suspend fun pairOnce(timeoutMs: Long): PairResult {
        start()
        val terminal = setOf(LinkState.CONNECTED, LinkState.UNLINKED, LinkState.OUTDATED, LinkState.BROKEN, LinkState.REPLACED)
        return when (withTimeoutOrNull(timeoutMs) { state.first { it in terminal } }) {
            LinkState.CONNECTED -> PairResult.Linked(pcName.value)
            LinkState.UNLINKED -> PairResult.Expired
            LinkState.OUTDATED -> PairResult.Outdated
            null -> { stop(sendBye = false); PairResult.Failed }
            else -> PairResult.Failed
        }
    }

    private companion object {
        const val PING_MS = 30_000L
        const val MAX_MISSED = 2
        const val FIRST_BACKOFF = 2_000L
        const val MAX_BACKOFF = 60_000L
        const val MAX_BAD = 3
    }
}
