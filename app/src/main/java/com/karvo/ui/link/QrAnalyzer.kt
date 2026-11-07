package com.karvo.ui.link

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/** Reads a QR from a camera luminance plane; only Karvo link codes are returned. */
fun decodeKarvoQr(y: ByteArray, width: Int, height: Int, rowStride: Int): String? = runCatching {
    val source = PlanarYUVLuminanceSource(y, rowStride, height, 0, 0, width, height, false)
    QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))).text
}.getOrNull()?.takeIf { it.startsWith("karvo:link?") }

/** CameraX analyzer: calls [onCode] once with the first Karvo link code it sees. */
class QrAnalyzer(private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer {
    @Volatile private var done = false

    override fun analyze(image: ImageProxy) {
        image.use {
            if (done) return
            val plane = it.planes[0]
            val bytes = ByteArray(plane.buffer.remaining()).also { b -> plane.buffer.get(b) }
            decodeKarvoQr(bytes, it.width, it.height, plane.rowStride)?.let { text -> done = true; onCode(text) }
        }
    }
}
