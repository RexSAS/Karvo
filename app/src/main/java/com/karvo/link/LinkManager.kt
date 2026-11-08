package com.karvo.link

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/** What the manager needs from a link (LinkClient in the app, fakes in tests). */
interface LinkPeer {
    val roomId: String
    val state: StateFlow<LinkState>
    val lastCommandAt: StateFlow<Instant?>
    val pcName: StateFlow<String>
    fun start()
    suspend fun stop(sendBye: Boolean = true)
    suspend fun unpair()
    suspend fun pairOnce(timeoutMs: Long = 15_000): PairResult
}

/** What the Linked PCs screen needs from the manager. */
interface LinkControl {
    val states: StateFlow<Map<String, LinkState>>
    suspend fun pair(qr: LinkQr): PairResult
    suspend fun remove(roomId: String)
}

/** Shows or hides the "Karvo is linked to …" foreground notification. */
interface ServiceStarter {
    fun start(names: List<String>)
    fun update(names: List<String>)
    fun stop()
}

/**
 * Runs one link per linked PC (spec "Phone behaviour"): reachable while Karvo is in the foreground; once a PC is
 * connected the foreground service keeps it alive until Disconnect, the PC leaves, or [idle] without a command.
 * Everything runs on [scope]'s (main) thread.
 */
class LinkManager(
    private val store: PairingStore,
    private val newPeer: (LinkedPc) -> LinkPeer,
    private val service: ServiceStarter,
    private val scope: CoroutineScope,
    private val now: () -> Instant = Instant::now,
    private val idle: Duration = Duration.ofMinutes(30),
) : LinkControl {
    private class Managed(val peer: LinkPeer, val jobs: List<Job>, var connectedAt: Instant? = null, var wasConnected: Boolean = false)

    private val peers = mutableMapOf<String, Managed>()
    private val pending = mutableSetOf<String>() // pairing in progress: not in the store yet
    private var foreground = false
    private var serviceRunning = false
    private var graceJob: Job? = null
    private val _states = MutableStateFlow<Map<String, LinkState>>(emptyMap())
    override val states: StateFlow<Map<String, LinkState>> = _states.asStateFlow()

    init {
        scope.launch { store.pcs.collect(::sync) }
        scope.launch { while (true) { delay(60_000); checkIdle() } }
    }

    private fun sync(pcs: List<LinkedPc>) {
        val ids = pcs.map { it.roomId }.toSet()
        (peers.keys - ids - pending).forEach { id -> peers.remove(id)?.let { m -> m.jobs.forEach(Job::cancel); scope.launch { m.peer.stop() } } }
        pcs.filter { it.secret != null && it.roomId !in peers }.forEach { pc ->
            val peer = attach(newPeer(pc))
            if (foreground) peer.start()
        }
        publish()
    }

    private fun attach(peer: LinkPeer): LinkPeer {
        val jobs = listOf(
            scope.launch { peer.state.collect { onState(peer, it) } },
            scope.launch { peer.pcName.drop(1).collect { if (peer.roomId !in pending) store.rename(peer.roomId, it) } },
        )
        peers[peer.roomId] = Managed(peer, jobs)
        return peer
    }

    private fun onState(peer: LinkPeer, s: LinkState) {
        val m = peers[peer.roomId] ?: return
        when (s) {
            LinkState.CONNECTED -> {
                // Android forbids starting a foreground service from the background: such a link waits for the app.
                if (!foreground && !serviceRunning) { scope.launch { peer.stop(sendBye = true) }; return }
                m.connectedAt = now(); m.wasConnected = true
            }
            LinkState.UNLINKED -> if (peer.roomId !in pending) {
                m.wasConnected = false
                peers.remove(peer.roomId)?.jobs?.forEach(Job::cancel)
                scope.launch { store.remove(peer.roomId) }
            }
            LinkState.REPLACED, LinkState.BROKEN, LinkState.OUTDATED -> { m.connectedAt = null; m.wasConnected = false }
            else -> m.connectedAt = null
        }
        publish()
    }

    private fun publish() {
        _states.value = peers.mapValues { it.value.peer.state.value }
        val names = peers.values.filter { it.peer.state.value == LinkState.CONNECTED }.map { it.peer.pcName.value }
        // A connected PC that drops (network blip) gets a short grace with the service kept, so it can come back.
        val reconnecting = peers.values.filter { it.wasConnected && it.peer.state.value in RECONNECTING }
        if (names.isEmpty() && reconnecting.isNotEmpty() && serviceRunning) {
            if (graceJob?.isActive != true) graceJob = scope.launch { delay(GRACE_MS); endGrace() }
            return
        }
        graceJob?.cancel()
        when {
            names.isEmpty() && serviceRunning -> { service.stop(); serviceRunning = false }
            names.isNotEmpty() && serviceRunning -> service.update(names)
            names.isNotEmpty() && foreground -> { service.start(names); serviceRunning = true }
        }
    }

    private fun endGrace() {
        peers.values.filter { it.wasConnected && it.peer.state.value in RECONNECTING }.forEach {
            it.wasConnected = false
            if (!foreground) scope.launch { it.peer.stop(sendBye = false) }
        }
        publish()
    }

    private fun checkIdle() {
        if (foreground) return // while Karvo is open the phone stays reachable
        val active = peers.values.filter { it.peer.state.value == LinkState.CONNECTED }
        if (active.isEmpty()) return
        val last = active.mapNotNull { listOfNotNull(it.connectedAt, it.peer.lastCommandAt.value).maxOrNull() }.maxOrNull() ?: return
        if (Duration.between(last, now()) >= idle) disconnectAll()
    }

    fun onForeground() {
        foreground = true
        peers.values.forEach { it.peer.start() }
    }

    fun onBackground() {
        foreground = false
        peers.values.filter { it.peer.state.value != LinkState.CONNECTED }.forEach { scope.launch { it.peer.stop(sendBye = false) } }
    }

    /** Notification "Disconnect", idle timeout: links come back the next time Karvo is opened. */
    fun disconnectAll() {
        peers.values.forEach { scope.launch { it.peer.stop(sendBye = true) } }
    }

    override suspend fun remove(roomId: String) {
        store.remove(roomId) // forget it first: the network part below may be slow or cancelled
        peers.remove(roomId)?.let { m -> m.jobs.forEach(Job::cancel); m.peer.unpair() }
        publish()
    }

    /** Pairs with the PC from a scanned QR and keeps the link on success. */
    override suspend fun pair(qr: LinkQr): PairResult {
        val roomId = LinkCrypto.b64url(qr.roomId)
        pending += roomId
        val peer = attach(newPeer(LinkedPc(roomId, qr.secret, qr.pcName, now())))
        var kept = false
        try {
            val result = peer.pairOnce()
            if (result is PairResult.Linked) {
                store.add(LinkQr(qr.roomId, qr.secret, result.pcName))
                store.pcs.first { list -> list.any { it.roomId == roomId } }
                kept = true
            }
            return result
        } finally {
            // Also when the screen is left mid-pairing: never leave a link running that Linked PCs doesn't show.
            withContext(NonCancellable) {
                if (!kept) { peers.remove(roomId)?.jobs?.forEach(Job::cancel); peer.stop(sendBye = false) }
                pending -= roomId
                publish()
            }
        }
    }

    private companion object {
        val RECONNECTING = setOf(LinkState.OFFLINE, LinkState.CONNECTING)
        const val GRACE_MS = 2 * 60_000L
    }
}
