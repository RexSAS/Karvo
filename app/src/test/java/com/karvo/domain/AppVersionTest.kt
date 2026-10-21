package com.karvo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {
    @Test fun parsesTagsAndNames() {
        assertEquals(listOf(1, 2, 0), parseVersion("v1.2.0")!!.parts)
        assertEquals(listOf(1, 2), parseVersion("1.2")!!.parts)
    }

    @Test fun rejectsNonNumeric() {
        assertNull(parseVersion("1.2-beta"))
        assertNull(parseVersion(""))
        assertNull(parseVersion("v"))
    }

    @Test fun comparesWithZeroPadding() {
        assertEquals(0, parseVersion("1.2")!!.compareTo(parseVersion("1.2.0")!!))
        assertTrue(parseVersion("1.10")!! > parseVersion("1.9")!!)
        assertTrue(parseVersion("1.2.1")!! > parseVersion("1.2")!!)
    }
}
