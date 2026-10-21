package com.masterski.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseParseTest {
    private fun json(tag: String, assets: String) = """
        {"tag_name":"$tag","name":"MasterSki 1.2","body":"What's new\n- stuff","assets":[$assets]}
    """.trimIndent()

    private val apk = """{"name":"MasterSki-1.2.apk","size":2434500,"browser_download_url":"https://github.com/RexSAS/MasterSki/releases/download/v1.2.0/MasterSki-1.2.apk"}"""
    private val notes = """{"name":"notes.txt","size":10,"browser_download_url":"https://example.com/notes.txt"}"""
    private val other = """{"name":"other.apk","size":5,"browser_download_url":"https://example.com/other.apk"}"""

    @Test fun picksFirstApkAsset() {
        val r = parseRelease(json("v1.2.0", "$notes,$apk,$other"))!!
        assertEquals(listOf(1, 2, 0), r.version.parts)
        assertEquals("https://github.com/RexSAS/MasterSki/releases/download/v1.2.0/MasterSki-1.2.apk", r.apkUrl)
        assertEquals(2434500L, r.apkSize)
        assertEquals("MasterSki 1.2", r.title)
        assertEquals("What's new\n- stuff", r.notes)
    }

    @Test fun nullWithoutApkAsset() = assertNull(parseRelease(json("v1.2.0", notes)))

    @Test fun nullForNonVersionTag() = assertNull(parseRelease(json("latest", apk)))

    @Test fun nullForBadJson() = assertNull(parseRelease("not json"))
}
