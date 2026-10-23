package com.karvo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class ProCodeTest {
    // A stand-in code: the real one must never appear in this public repo.
    private val hash = sha256Hex(canonicalProCode("KARVO-ABCD-1234"))

    @Test fun acceptsTheCodeIgnoringCaseSpacesAndDashes() {
        assertTrue(isProCode("KARVO-ABCD-1234", hash))
        assertTrue(isProCode("  karvo abcd-1234 ", hash))
    }

    @Test fun acceptsLookAlikeCharacters() {
        // KARV0 with a zero, I for 1, Z for 2.
        assertTrue(isProCode("KARV0-ABCD-IZ34", hash))
    }

    @Test fun rejectsOtherInput() {
        assertFalse(isProCode("KARVO-ABCD-1235", hash))
        assertFalse(isProCode("", hash))
        assertFalse(isProCode("---", sha256Hex("")))
    }

    @Test fun realHashIsASha256() = assertTrue(PRO_CODE_SHA256.matches(Regex("[0-9a-f]{64}")))

    /** Only runs where the git-ignored pro-code.txt exists, i.e. on the developer's machine. */
    @Test fun realCodeUnlocks() {
        val file = listOf(File("pro-code.txt"), File("../pro-code.txt")).firstOrNull { it.exists() }
        assumeTrue(file != null)
        assertTrue(isProCode(file!!.readText()))
        assertTrue(isProCode(file.readText().lowercase().replace('2', 'z')))
    }

    @Test fun formatsAsYouType() {
        assertEquals("KARVO", formatProCodeInput("karvo"))
        assertEquals("KARVO-Q", formatProCodeInput("karvoq"))
        assertEquals("KARVO-ABCD-E", formatProCodeInput("KARVO-ABCDe"))
        assertEquals("KARVO-ABCD-EFGH-JKLM", formatProCodeInput("karvo abcd efgh jklm extra"))
        // Pasting a dashed code keeps it as is.
        assertEquals("KARVO-ABCD-EFGH-JKLM", formatProCodeInput("KARVO-ABCD-EFGH-JKLM"))
    }

    @Test fun backspaceOverADashDeletesTheCharacterBeforeIt() {
        assertEquals("KARV", formatProCodeInput("KARVO", previous = "KARVO-"))
        assertEquals("KARVO-ABC", formatProCodeInput("KARVO-ABCD", previous = "KARVO-ABCD-"))
    }
}
