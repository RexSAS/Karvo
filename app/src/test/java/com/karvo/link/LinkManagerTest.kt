package com.karvo.link

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.karvo.testing.FakeSecretBox
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LinkManagerTest {
    @get:Rule val tmp = TemporaryFolder()

    private class FakePeer(override val roomId: String, name: String) : LinkPeer {
        override val state = MutableStateFlow(LinkState.OFFLINE)
        override val lastCommandAt = MutableStateFlow<Instant?>(null)
        override val pcName = MutableStateFlow(name)
        var started = 0; var stopped = 0; var unpaired = 0; var lastBye: Boolean? = null
        override fun start() { started++; if (state.value == LinkState.OFFLINE) state.value = LinkState.CONNECTING }
        override suspend fun stop(sendBye: Boolean) { stopped++; lastBye = sendBye; state.value = LinkState.OFFLINE }
        override suspend fun unpair() { unpaired++; state.value = LinkState.UNLINKED }
        override suspend fun pairOnce(timeoutMs: Long): PairResult { start(); state.value = LinkState.CONNECTED; return PairResult.Linked(pcName.value) }
    }
    private class FakeService : ServiceStarter {
        var running = false; var names = emptyList<String>(); var starts = 0
        override fun start(names: List<String>) { running = true; starts++; this.names = names }
        override fun update(names: List<String>) { this.names = names }
        override fun stop() { running = false }
    }

    private val box = FakeSecretBox()
    private val peers = mutableMapOf<String, FakePeer>()
    private val service = FakeService()
    private fun TestScope.setup(): Pair<PairingStore, LinkManager> {
        val store = PairingStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "link.preferences_pb") }, box)
        val m = LinkManager(store, { pc -> FakePeer(pc.roomId, pc.pcName).also { peers[pc.roomId] = it } }, service, backgroundScope,
            now = { Instant.ofEpochMilli(currentTime) })
        return store to m
    }
    private fun qr(name: String) = LinkQr(LinkCrypto.randomBytes(16), LinkCrypto.randomBytes(32), name)
    private fun TestScope.settle() = repeat(5) { runCurrent() }

    @Test fun `foreground starts a client per linked PC, skipping broken ones`() = runTest {
        val (store, m) = setup()
        store.add(qr("A")); store.add(qr("B")); settle()
        m.onForeground(); settle()
        assertEquals(2, peers.values.count { it.started > 0 })
        box.broken = true
        store.add(qr("C")); settle() // C (and now all) can't be unwrapped: no client for them
        assertEquals(2, peers.size)
    }

    @Test fun `background keeps connected links and stops the rest`() = runTest {
        val (store, m) = setup()
        store.add(qr("A")); store.add(qr("B")); settle()
        m.onForeground(); settle()
        val (a, b) = peers.values.toList()
        a.state.value = LinkState.CONNECTED; settle()
        assertTrue(service.running); assertEquals(listOf("A"), service.names)
        m.onBackground(); settle()
        assertEquals(0, a.stopped); assertEquals(1, b.stopped)
    }

    @Test fun `a link that connects while in the background without the service is dropped, not crashed`() = runTest {
        val (store, m) = setup()
        store.add(qr("A")); settle()
        m.onForeground(); settle()
        val a = peers.values.single()
        m.onBackground(); settle()
        a.state.value = LinkState.CONNECTED; settle()
        assertFalse(service.running)
        assertEquals(LinkState.OFFLINE, a.state.value)
    }

    @Test fun `the service stops when the last PC disconnects`() = runTest {
        val (store, m) = setup()
        store.add(qr("A")); settle(); m.onForeground(); settle()
        val a = peers.values.single()
        a.state.value = LinkState.CONNECTED; settle()
        a.state.value = LinkState.OFFLINE; settle()
        assertFalse(service.running)
    }

    @Test fun `30 minutes without a command disconnects everything`() = runTest {
        val (store, m) = setup()
        store.add(qr("A")); settle(); m.onForeground(); settle()
        val a = peers.values.single()
        a.state.value = LinkState.CONNECTED; settle()
        advanceTimeBy(29 * 60_000L); settle()
        assertEquals(0, a.stopped)
        advanceTimeBy(2 * 60_000L); settle()
        assertEquals(1, a.stopped); assertEquals(true, a.lastBye)
    }

    @Test fun `an unlinked PC is removed from the list, a remove unpairs it`() = runTest {
        val (store, m) = setup()
        store.add(qr("A")); store.add(qr("B")); settle(); m.onForeground(); settle()
        val (a, b) = peers.values.toList()
        a.state.value = LinkState.UNLINKED; settle()
        assertEquals(listOf("B"), store.pcs.first().map { it.pcName })
        m.remove(b.roomId); settle()
        assertEquals(1, b.unpaired)
        assertTrue(store.pcs.first().isEmpty())
    }

    @Test fun `pairing saves the PC under the name it announced and keeps that client`() = runTest {
        val (store, m) = setup()
        m.onForeground(); settle()
        val result = m.pair(qr("Chrome on Windows"))
        settle()
        assertEquals(PairResult.Linked("Chrome on Windows"), result)
        assertEquals(listOf("Chrome on Windows"), store.pcs.first().map { it.pcName })
        assertEquals(1, peers.size)
        assertTrue(service.running)
    }
}
