package com.karvo.data.moments

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Native graphics, so the design is really drawn and its pixels can be read.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotobombTest {
    private fun photo(color: Int) = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
    private fun card(color: Int) = renderShareCard(ApplicationProvider.getApplicationContext(), photo(color), ShareStyle.Photobomb, "13 Nov 2026")

    // A spot in Kevin's lens, away from its reflection bars: (300, 300) in his drawing, placed as ShareCard places him.
    private val lensX = 876
    private val lensY = 1681

    @Test fun is_story_sized_whatever_the_photo() {
        val card = card(Color.RED)
        assertEquals(1080, card.width)
        assertEquals(1920, card.height)
    }

    @Test fun his_goggles_show_the_shared_photo() {
        val red = card(Color.RED).getPixel(lensX, lensY)
        val green = card(Color.GREEN).getPixel(lensX, lensY)
        assertTrue("a red photo makes the lens red: ${Integer.toHexString(red)}", Color.red(red) > Color.green(red) + 80)
        assertTrue("a green photo makes the lens green: ${Integer.toHexString(green)}", Color.green(green) > Color.red(green) + 80)
    }

    @Test fun the_photo_sits_where_the_editor_put_it() {
        val card = card(Color.RED)
        // Middle of the photo area is the photo; above it is the dimmed, blurred copy (darker).
        assertEquals(Color.RED, card.getPixel(200, 900))
        assertTrue(Color.red(card.getPixel(200, 100)) < 230)
    }
}
