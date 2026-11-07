package com.karvo.link

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class PairingStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    /** Reversible fake: the real one wraps with an Android Keystore key. */
    private class XorBox(var broken: Boolean = false) : SecretBox {
        override fun seal(plain: ByteArray) = plain.map { (it.toInt() xor 0x5a).toByte() }.toByteArray()
        override fun open(sealed: ByteArray): ByteArray { if (broken) error("key gone"); return seal(sealed) }
    }

    private fun store(box: SecretBox) = PairingStore(PreferenceDataStoreFactory.create { File(tmp.root, "link.preferences_pb") }, box) { Instant.parse("2026-11-08T10:00:00Z") }
    private val qr = LinkQr(LinkCrypto.randomBytes(16), LinkCrypto.randomBytes(32), "Chrome on Windows")

    @Test fun `add, rename and remove`() = runTest {
        val s = store(XorBox())
        s.add(qr)
        val pc = s.pcs.first().single()
        assertEquals(LinkCrypto.b64url(qr.roomId), pc.roomId)
        assertArrayEquals(qr.secret, pc.secret)
        assertEquals("Chrome on Windows", pc.pcName)
        s.rename(pc.roomId, "Edge on Windows")
        assertEquals("Edge on Windows", s.pcs.first().single().pcName)
        s.remove(pc.roomId)
        assertTrue(s.pcs.first().isEmpty())
    }

    @Test fun `a secret that can't be unwrapped shows as broken, no crash`() = runTest {
        val box = XorBox()
        val s = store(box)
        s.add(qr)
        box.broken = true
        val pc = s.pcs.first().single()
        assertNull(pc.secret)
        assertEquals("Chrome on Windows", pc.pcName)
    }
}
