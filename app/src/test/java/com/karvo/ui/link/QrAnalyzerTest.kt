package com.karvo.ui.link

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrAnalyzerTest {
    /** A luminance plane (one byte per pixel) with the QR drawn black on white, like a camera's Y plane. */
    private fun plane(text: String, size: Int = 400): ByteArray {
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        return ByteArray(size * size) { i -> if (m.get(i % size, i / size)) 0 else -1 }
    }

    @Test fun `reads a Karvo link code`() {
        val text = "karvo:link?v=1&r=AAAAAAAAAAAAAAAAAAAAAA&k=BBBB&n=PC"
        assertEquals(text, decodeKarvoQr(plane(text), 400, 400, 400))
    }

    @Test fun `ignores other codes`() {
        assertNull(decodeKarvoQr(plane("https://example.com"), 400, 400, 400))
    }
}
