package com.mowtiie.mesgard.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactSearchTextTest {
    @Test
    fun `trims the text and keeps ordinary names as they are`() {
        assertEquals("Ana Cruz", escapeForLike("  Ana Cruz "))
    }

    @Test
    fun `wildcards and request separators are taken out`() {
        assertEquals("anb", escapeForLike("a%n*b,()"))
    }

    @Test
    fun `an underscore is searched literally`() {
        assertEquals("a\\_b", escapeForLike("a_b"))
    }
}
