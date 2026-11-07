package com.karvo.link

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports of karvo-web/test/linkProtocol.test.js: the phone must enforce exactly what the PC enforces. */
class LinkProtocolTest {
    private val roomId = LinkCrypto.randomBytes(16)
    private val k = LinkCrypto.deriveKeys(LinkCrypto.randomBytes(32), roomId)
    private fun pcChannel() = LinkChannel(k.pcToPhone, k.phoneToPc, roomId, 1, 2)
    private fun phoneChannel() = LinkChannel(k.phoneToPc, k.pcToPhone, roomId, 2, 1)
    private val pc = pcChannel()
    private val phone = phoneChannel()
    private fun shake() { phone.decode(pc.hello("web", "PC")); pc.decode(phone.hello("3.1", "Pixel")) }
    private fun raw(json: String) = LinkCrypto.seal(k.pcToPhone, roomId, 1, json.toByteArray())

    @Test fun `hello exchange then messages decode in order`() {
        val h = phone.decode(pc.hello("web", "PC"))
        assertEquals("hello", h.type); assertEquals("PC", h.json.getString("name")); assertTrue(h.isNewHello)
        pc.decode(phone.hello("3.1", "Pixel"))
        for (n in 1..3) {
            val m = phone.decode(pc.encode("command", JSONObject().put("id", n)))
            assertEquals("command", m.type); assertEquals(n, m.json.getInt("q")); assertEquals(n, m.json.getInt("id"))
        }
    }

    @Test fun `a replayed message is rejected`() {
        shake()
        phone.decode(pc.encode("ping"))
        val second = pc.encode("ping")
        phone.decode(second)
        assertThrows(ProtocolException::class.java) { phone.decode(second) }
    }

    @Test fun `tampered frames and messages before hello are rejected`() {
        val hello = pc.hello("web", "PC").also { it[15] = (it[15].toInt() xor 1).toByte() }
        assertThrows(ProtocolException::class.java) { phone.decode(hello) }
        assertThrows(ProtocolException::class.java) { phone.decode(raw("""{"s":null,"q":1,"t":"ping"}""")) }
    }

    @Test fun `another protocol version is a version error`() {
        val e = assertThrows(ProtocolException::class.java) {
            phone.decode(raw("""{"s":null,"q":0,"t":"hello","v":2,"app":"web-9","session":"x"}"""))
        }
        assertEquals("version", e.code); assertEquals(2, e.peerVersion)
    }

    @Test fun `encode before the peer hello throws`() {
        assertThrows(ProtocolException::class.java) { pc.encode("ping") }
    }

    @Test fun `a repeated hello of the current session is ignored and does not reset the sequence`() {
        val hello = pc.hello("web", "PC")
        assertTrue(phone.decode(hello).isNewHello)
        pc.decode(phone.hello("3.1", "P"))
        val m = pc.encode("ping")
        phone.decode(m)
        assertFalse(phone.decode(hello).isNewHello)
        assertThrows(ProtocolException::class.java) { phone.decode(m) }
    }

    @Test fun `a reconnected peer is accepted and its old frames are not`() {
        shake()
        val old = pc.encode("ping")
        val pc2 = pcChannel()
        assertTrue(phone.decode(pc2.hello("web", "PC")).isNewHello)
        pc2.decode(phone.hello("3.1", "P"))
        assertThrows(ProtocolException::class.java) { phone.decode(old) }
        assertEquals("ping", phone.decode(pc2.encode("ping")).type)
    }

    @Test fun `an old hello cannot be used to replay newer frames`() {
        val oldHello = pcChannel().hello("web", "PC")
        shake()
        val m = pc.encode("command", JSONObject().put("id", 1))
        phone.decode(m)
        phone.decode(oldHello)
        assertThrows(ProtocolException::class.java) { phone.decode(m) }
    }

    @Test fun `switching hellos A to B to A cannot replay A-session commands`() {
        val helloA = pc.hello("web", "PC")
        phone.decode(helloA)
        pc.decode(phone.hello("3.1", "P"))
        val cmd = pc.encode("command", JSONObject().put("id", 1))
        phone.decode(cmd)
        phone.decode(pcChannel().hello("web", "PC"))
        assertThrows(ProtocolException::class.java) { phone.decode(helloA) }
        assertThrows(ProtocolException::class.java) { phone.decode(cmd) }
    }

    @Test fun `frames from the web app decode on the phone`() {
        // A hello exactly as karvo-web/link/protocol.js writes it (field order and extra fields included).
        val js = """{"app":"web-2","name":"Chrome on Linux","s":null,"f":"abc","q":0,"t":"hello","v":1,"session":"abc"}"""
        val h = phone.decode(raw(js))
        assertEquals("Chrome on Linux", h.json.getString("name"))
    }
}
