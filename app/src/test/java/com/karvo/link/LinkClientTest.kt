package com.karvo.link

import com.karvo.data.Lesson
import com.karvo.testing.FakeLessonRepository
import com.karvo.testing.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LinkClientTest {
    private class FakeSocket(val url: String, val listener: RelaySocketListener) : RelaySocket {
        val sent = mutableListOf<ByteArray>()
        var closedWith: Int? = null
        override fun send(bytes: ByteArray): Boolean { if (closedWith != null) return false; sent += bytes; return true }
        override fun close(code: Int) { closedWith = code }
    }
    private class FakeFactory : RelaySocketFactory {
        val sockets = mutableListOf<FakeSocket>()
        val deleted = mutableListOf<String>()
        override fun open(url: String, listener: RelaySocketListener) = FakeSocket(url, listener).also { sockets += it }
        override suspend fun delete(url: String): Int { deleted += url; return 204 }
    }

    private val roomId = LinkCrypto.randomBytes(16)
    private val secret = LinkCrypto.randomBytes(32)
    private val keys = LinkCrypto.deriveKeys(secret, roomId)
    private val pcRecord = LinkedPc(LinkCrypto.b64url(roomId), secret, "Chrome on Windows", Instant.EPOCH)
    private val lessons = FakeLessonRepository(listOf(Lesson(1, LocalDate.parse("2026-01-05"), 60, "Anna")))
    private val handler = LinkCommandHandler(lessons, FakeSettingsRepository(initial = 3000L), com.karvo.testing.FakeDayNoteRepository(), { it.name }, { it.name })
    private val factory = FakeFactory()
    private fun TestScope.client() = LinkClient(pcRecord, handler, factory, backgroundScope, "3.1", "Pixel 8", now = { Instant.EPOCH })
    private fun pcChannel() = LinkChannel(keys.pcToPhone, keys.phoneToPc, roomId, 1, 2)
    private val sock get() = factory.sockets.last()
    /** Runs what is due now, a few rounds (Room-like suspensions), without jumping virtual time to the ping timer. */
    private fun TestScope.settle() = repeat(5) { runCurrent() }

    /** Opens the socket and does the hello exchange from the PC side; returns the PC channel. */
    private fun TestScope.greet(pc: LinkChannel = pcChannel()): LinkChannel {
        sock.listener.onOpen(); runCurrent()
        pc.decode(sock.sent.first())
        sock.listener.onMessage(pc.hello("web-2", "Chrome on Windows")); runCurrent()
        return pc
    }
    private fun LinkChannel.received(from: Int = 0) = sock.sent.drop(from).map { decode(it) }

    @Test fun `connects as the phone and answers the PC hello with hello and a snapshot`() = runTest {
        val c = client(); c.start(); runCurrent()
        assertEquals("wss://link.rxsas.it/room/${pcRecord.roomId}?role=phone&auth=${LinkCrypto.b64url(keys.authPhone)}", sock.url)
        val pc = greet()
        assertEquals(LinkState.CONNECTED, c.state.value)
        val got = pc.received(1)
        assertEquals(listOf("hello", "snapshot"), got.map { it.type }.take(2))
        assertEquals(1, got.first { it.type == "snapshot" }.json.getJSONArray("lessons").length())
    }

    @Test fun `two commands back to back are applied and acked in order`() = runTest {
        val c = client(); c.start(); runCurrent()
        val pc = greet()
        val before = sock.sent.size
        val add = JSONObject().put("lesson", JSONObject().put("date", "2026-01-06").put("durationMinutes", 90).put("student", "Bo").put("notes", JSONObject.NULL).put("paidOn", JSONObject.NULL))
        sock.listener.onMessage(pc.encode("command", JSONObject().put("id", 1).put("op", "addLesson").put("args", add)))
        sock.listener.onMessage(pc.encode("command", JSONObject().put("id", 2).put("op", "markPaid").put("args", JSONObject().put("month", "2026-01").put("paidOn", "2026-02-01"))))
        settle()
        val acks = pc.received(before).filter { it.type == "ack" }
        assertEquals(listOf(1, 2), acks.map { it.json.getInt("id") })
        assertTrue(acks.all { it.json.getBoolean("ok") })
        assertTrue(lessons.allLessons().all { it.paidOn == LocalDate.parse("2026-02-01") })
        assertEquals(2, lessons.allLessons().size)
    }

    @Test fun `a refused command gets ok false with the reason`() = runTest {
        val c = client(); c.start(); runCurrent()
        val pc = greet()
        val before = sock.sent.size
        sock.listener.onMessage(pc.encode("command", JSONObject().put("id", 5).put("op", "deleteLesson").put("args", JSONObject().put("id", 99))))
        settle()
        val ack = pc.received(before).first { it.type == "ack" }.json
        assertFalse(ack.getBoolean("ok")); assertEquals("LessonGone", ack.getString("error"))
    }

    @Test fun `switching hellos A to B to A cannot re-apply a command`() = runTest {
        val c = client(); c.start(); runCurrent()
        val a = pcChannel()
        sock.listener.onOpen(); runCurrent()
        a.decode(sock.sent.first())
        val helloA = a.hello("web-2", "PC")
        sock.listener.onMessage(helloA); runCurrent()
        val cmd = a.encode("command", JSONObject().put("id", 1).put("op", "deleteLesson").put("args", JSONObject().put("id", 1)))
        sock.listener.onMessage(cmd); settle()
        lessons.save(Lesson(0, LocalDate.parse("2026-01-05"), 60, "Anna"))
        sock.listener.onMessage(pcChannel().hello("web-2", "PC")); runCurrent()
        sock.listener.onMessage(helloA); sock.listener.onMessage(cmd); settle()
        assertEquals(1, lessons.allLessons().size)
    }

    @Test fun `reconnects with backoff from 2 up to 60 seconds`() = runTest {
        val c = client(); c.start(); runCurrent()
        val gaps = mutableListOf<Long>()
        repeat(7) {
            val n = factory.sockets.size
            sock.listener.onClosed(1006, opened = false, httpStatus = null); runCurrent()
            var waited = 0L
            while (factory.sockets.size == n) { advanceTimeBy(1000); runCurrent(); waited += 1000 }
            gaps += waited
        }
        assertEquals(listOf(2000L, 4000, 8000, 16000, 32000, 60000, 60000), gaps)
    }

    @Test fun `replaced, unlinked, deleted room and unpaired end the link`() = runTest {
        for ((close, expected) in listOf(
            Triple(4000, true, null as Int?) to LinkState.REPLACED,
            Triple(4002, true, null) to LinkState.UNLINKED,
            Triple(1006, false, 404) to LinkState.UNLINKED,
            Triple(1006, false, 401) to LinkState.BROKEN,
        )) {
            val c = client(); c.start(); runCurrent()
            val n = factory.sockets.size
            sock.listener.onClosed(close.first, close.second, close.third); runCurrent(); advanceTimeBy(120_000); runCurrent()
            assertEquals(expected, c.state.value)
            assertEquals(n, factory.sockets.size)
        }
        val c = client(); c.start(); runCurrent()
        val pc = greet()
        sock.listener.onMessage(pc.encode("unpaired")); runCurrent()
        assertEquals(LinkState.UNLINKED, c.state.value)
    }

    @Test fun `stop sends bye before closing`() = runTest {
        val c = client(); c.start(); runCurrent()
        val pc = greet()
        c.stop(); runCurrent()
        assertEquals("bye", pc.received(1).last().type)
        assertEquals(1000, sock.closedWith)
    }

    @Test fun `unpair tells the PC, closes and deletes the room`() = runTest {
        val c = client(); c.start(); runCurrent()
        val pc = greet()
        c.unpair(); runCurrent()
        assertEquals("unpaired", pc.received(1).last().type)
        assertEquals(listOf("https://link.rxsas.it/room/${pcRecord.roomId}?role=phone&auth=${LinkCrypto.b64url(keys.authPhone)}"), factory.deleted)
    }

    @Test fun `pairing waits for the PC hello, or reports an expired code`() = runTest {
        val ok = async { client().pairOnce() }
        runCurrent(); greet()
        assertEquals(PairResult.Linked("Chrome on Windows"), ok.await())
        val gone = async { client().pairOnce() }
        runCurrent(); sock.listener.onClosed(1006, false, 404)
        assertEquals(PairResult.Expired, gone.await())
    }
}
