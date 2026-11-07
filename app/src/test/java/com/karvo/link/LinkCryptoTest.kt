package com.karvo.link

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.GeneralSecurityException

/** The same vectors the PC web app tests (karvo-web/test/vectors/link-v1.json), so both sides encrypt identically. */
class LinkCryptoTest {
    private val v = JSONObject(javaClass.getResource("/link-v1.json")!!.readText())
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private val secret = hex(v.getString("secret"))
    private val roomId = hex(v.getString("roomId"))
    private val nonce = hex(v.getString("nonce"))
    private val plaintext = v.getString("plaintext").toByteArray()

    @Test fun `keys and auth tokens match the vectors`() {
        val k = LinkCrypto.deriveKeys(secret, roomId)
        assertEquals(v.getString("pcToPhone"), k.pcToPhone.hex())
        assertEquals(v.getString("phoneToPc"), k.phoneToPc.hex())
        assertEquals(v.getString("authPc"), k.authPc.hex())
        assertEquals(v.getString("authPhone"), k.authPhone.hex())
        assertEquals(v.getString("authPcSha256"), LinkCrypto.sha256Hex(k.authPc))
        assertEquals(v.getString("authPhoneSha256"), LinkCrypto.sha256Hex(k.authPhone))
    }

    @Test fun `frames match the vectors in both directions`() {
        val k = LinkCrypto.deriveKeys(secret, roomId)
        assertEquals(v.getString("framePcToPhone"), LinkCrypto.seal(k.pcToPhone, roomId, 1, plaintext, nonce).hex())
        assertEquals(v.getString("framePhoneToPc"), LinkCrypto.seal(k.phoneToPc, roomId, 2, plaintext, nonce).hex())
    }

    @Test fun `open reverses seal and rejects anything altered`() {
        val k = LinkCrypto.deriveKeys(secret, roomId)
        val frame = LinkCrypto.seal(k.pcToPhone, roomId, 1, plaintext)
        assertArrayEquals(plaintext, LinkCrypto.open(k.pcToPhone, roomId, 1, frame))
        val bad = frame.copyOf().also { it[20] = (it[20].toInt() xor 1).toByte() }
        assertThrows(GeneralSecurityException::class.java) { LinkCrypto.open(k.pcToPhone, roomId, 1, bad) }
        assertThrows(GeneralSecurityException::class.java) { LinkCrypto.open(k.pcToPhone, roomId, 2, frame) }
        assertThrows(GeneralSecurityException::class.java) { LinkCrypto.open(k.pcToPhone, ByteArray(16), 1, frame) }
        assertThrows(GeneralSecurityException::class.java) { LinkCrypto.open(k.phoneToPc, roomId, 1, frame) }
    }

    @Test fun `base64url without padding`() {
        assertEquals("-_8AAQ", LinkCrypto.b64url(byteArrayOf(-5, -1, 0, 1)))
        assertArrayEquals(byteArrayOf(-5, -1, 0, 1), LinkCrypto.fromB64url("-_8AAQ"))
    }
}
