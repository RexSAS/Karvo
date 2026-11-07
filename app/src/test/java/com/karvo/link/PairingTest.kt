package com.karvo.link

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PairingTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test fun `parses a QR made by the PC web app`() {
        // Made with karvo-web/link/pairing.js newPairing('Chrome on Windows').
        val qr = parseLinkQr("karvo:link?v=1&r=8IgUR16rwE-f1zmZDjyysA&k=SiHVynCoM1IRY9gZmeez6-KHJ4R6GLDLlHcJCA17ygE&n=Chrome%20on%20Windows")
        assertArrayEquals(hex("f08814475eabc04f9fd739990e3cb2b0"), qr.roomId)
        assertArrayEquals(hex("4a21d5ca70a833521163d81999e7b3ebe28727847a18b0cb947709080d7bca01"), qr.secret)
        assertEquals("Chrome on Windows", qr.pcName)
    }

    @Test fun `names with accents survive`() {
        val r = LinkCrypto.b64url(ByteArray(16)); val k = LinkCrypto.b64url(ByteArray(32))
        assertEquals("Chrome su Windows · è", parseLinkQr("karvo:link?v=1&r=$r&k=$k&n=Chrome%20su%20Windows%20%C2%B7%20%C3%A8").pcName)
    }

    @Test fun `wrong scheme, version or key length are refused`() {
        val r = LinkCrypto.b64url(ByteArray(16)); val k = LinkCrypto.b64url(ByteArray(32))
        assertEquals("not_karvo", assertThrows(IllegalArgumentException::class.java) { parseLinkQr("https://x.y/?v=1&r=$r&k=$k") }.message)
        assertEquals("version", assertThrows(IllegalArgumentException::class.java) { parseLinkQr("karvo:link?v=2&r=$r&k=$k&n=x") }.message)
        assertEquals("damaged", assertThrows(IllegalArgumentException::class.java) {
            parseLinkQr("karvo:link?v=1&r=${LinkCrypto.b64url(ByteArray(15))}&k=$k&n=x")
        }.message)
        assertEquals("damaged", assertThrows(IllegalArgumentException::class.java) { parseLinkQr("karvo:link?v=1&r=!!&k=$k&n=x") }.message)
    }
}
