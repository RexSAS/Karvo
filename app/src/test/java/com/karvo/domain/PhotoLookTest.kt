package com.karvo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoLookTest {
    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun Int.r() = (this shr 16) and 0xFF
    private fun Int.g() = (this shr 8) and 0xFF
    private fun Int.b() = this and 0xFF
    private fun Int.saturation(): Double {
        val max = maxOf(r(), g(), b())
        return if (max == 0) 0.0 else (max - minOf(r(), g(), b())) / max.toDouble()
    }

    @Test fun toneCurveKeepsEndpointsAndOrder() {
        assertEquals(0.0, PhotoLook.tone(0.0), 1e-9)
        assertEquals(1.0, PhotoLook.tone(1.0), 1e-9)
        (1..255).forEach { assertTrue("curve must not go down at $it", PhotoLook.tone(it / 255.0) >= PhotoLook.tone((it - 1) / 255.0)) }
    }

    @Test fun shadowsLiftAndHighlightsEase() {
        assertTrue(PhotoLook.apply(rgb(40, 40, 40)).g() > 40)
        assertTrue(PhotoLook.apply(rgb(235, 235, 235)).g() < 235)
    }

    @Test fun blackAndWhiteStayPureAndAlphaIsKept() {
        assertEquals(rgb(0, 0, 0), PhotoLook.apply(rgb(0, 0, 0)))
        assertEquals(rgb(255, 255, 255), PhotoLook.apply(rgb(255, 255, 255)))
        assertEquals(0x80, PhotoLook.apply((0x80 shl 24) or 0x404040) ushr 24)
    }

    @Test fun mutedColoursGetRicher() {
        assertTrue(PhotoLook.apply(rgb(110, 140, 120)).saturation() > rgb(110, 140, 120).saturation())
        assertTrue(PhotoLook.apply(rgb(90, 120, 160)).saturation() > rgb(90, 120, 160).saturation())
    }

    @Test fun strongColoursGetPunchierToo() {
        assertTrue(PhotoLook.apply(rgb(40, 90, 200)).saturation() > rgb(40, 90, 200).saturation())
    }

    @Test fun skinIsBoostedLessThanOtherMutedColours() {
        val skin = rgb(200, 160, 140)
        val sky = rgb(140, 160, 200)
        val skinGain = PhotoLook.apply(skin).saturation() - skin.saturation()
        val skyGain = PhotoLook.apply(sky).saturation() - sky.saturation()
        assertTrue(skinGain < skyGain + 0.05)
    }

    @Test fun midGreyGetsSlightlyWarmer() {
        val out = PhotoLook.apply(rgb(128, 128, 128))
        assertTrue(out.r() > out.b())
    }
}
