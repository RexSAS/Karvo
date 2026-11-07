package com.karvo.link

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
) {
    private class Managed(val peer: LinkPeer, val jobs: List<Job>, var connectedAt: Instant? = null)

    private val peers = mutableMapOf<String, Managed>()
    private val pending = mutableSetOf<String>() // pairing in progress: not in the store yet
    private var foreground = false
    private var serviceRunning = false
    private val _states = MutableStateFlow<Map<String, LinkState>>(emptyMap())
    val states: StateFlow<Map<String, LinkState>> = _states.asStateFlow()

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
                m.connectedAt = now()
            }
            LinkState.UNLINKED -> if (peer.roomId !in pending) {
                peers.remove(peer.roomId)?.jobs?.forEach(Job::cancel)
                scope.launch { store.remove(peer.roomId) }
            }
            else -> m.connectedAt = null
        }
        publish()
    }

    private fun publish() {
        _states.value = peers.mapValues { it.value.peer.state.value }
        val names = peers.values.filter { it.peer.state.value == LinkState.CONNECTED }.map { it.peer.pcName.value }
        when {
            names.isEmpty() && serviceRunning -> { service.stop(); serviceRunning = false }
            names.isNotEmpty() && serviceRunning -> service.update(names)
            names.isNotEmpty() && foreground -> { service.start(names); serviceRunning = true }
        }
    }

    private fun checkIdle() {
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

    suspend fun remove(roomId: String) {
        peers.remove(roomId)?.let { m -> m.jobs.forEach(Job::cancel); m.peer.unpair() }
        store.remove(roomId)
        publish()
    }

    /** Pairs with the PC from a scanned QR and keeps the link on success. */
    suspend fun pair(qr: LinkQr): PairResult {
        val roomId = LinkCrypto.b64url(qr.roomId)
        pending += roomId
        val peer = attach(newPeer(LinkedPc(roomId, qr.secret, qr.pcName, now())))
        val result = peer.pairOnce()
        if (result is PairResult.Linked) {
            store.add(LinkQr(qr.roomId, qr.secret, result.pcName))
            store.pcs.first { list -> list.any { it.roomId == roomId } }
        } else {
            peers.remove(roomId)?.jobs?.forEach(Job::cancel)
            peer.stop(sendBye = false)
        }
        pending -= roomId
        publish()
        return result
    }
}
