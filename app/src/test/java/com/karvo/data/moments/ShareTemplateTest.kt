package com.karvo.data.moments

import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

// Native graphics, so BitmapFactory decodes the real PNG pixels.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareTemplateTest {
    private val paper = 0xFFF7F4EE.toInt()
    private val magenta = 0xFFFF00FF.toInt()

    /** 200×300, transparent around a paper frame (20..180 × 40..280) with a window (40..160 × 80..260) and a date box. */
    private fun straightFrame(): IntArray = IntArray(200 * 300) { i ->
        val x = i % 200
        val y = i / 200
        when {
            x in 100 until 170 && y in 50 until 70 -> magenta
            x in 40 until 160 && y in 80 until 260 -> 0
            x in 20 until 180 && y in 40 until 280 -> paper
            else -> 0
        }
    }

    @Test fun readsWindowDateAndTiltOfAStraightFrame() {
        val t = readShareTemplate(straightFrame(), 200, 300)
        assertEquals(0f, t.tilt, 0.01f)
        assertEquals(TiltedBox(100f, 170f, 120f, 180f), t.window)
        assertEquals(TiltedBox(135f, 60f, 70f, 20f), t.date)
    }

    @Test fun paintsTheDateBoxOverWithTheFrameColour() {
        val t = readShareTemplate(straightFrame(), 200, 300)
        assertTrue(t.pixels.none { it == magenta })
        assertEquals(paper, t.pixels[60 * 200 + 135])
        // The outside stays transparent.
        assertEquals(0, t.pixels[5 * 200 + 5])
    }

    @Test fun shippedFrameFollowsTheRules() {
        val file = listOf(File("src/main/res/drawable-nodpi/share_frame.png"), File("app/src/main/res/drawable-nodpi/share_frame.png")).first { it.exists() }
        val image = BitmapFactory.decodeFile(file.path)
        val pixels = IntArray(image.width * image.height).also { image.getPixels(it, 0, image.width, 0, 0, image.width, image.height) }
        val t = readShareTemplate(pixels, image.width, image.height)
        assertEquals(1080 to 1920, t.width to t.height)
        // Tilted a little clockwise, with a big window and a date spot.
        assertTrue("tilt ${t.tilt}", t.tilt in 2f..7f)
        assertTrue("window ${t.window}", t.window.width > 600 && t.window.height > 600)
        assertNotNull(t.date)
    }
}
