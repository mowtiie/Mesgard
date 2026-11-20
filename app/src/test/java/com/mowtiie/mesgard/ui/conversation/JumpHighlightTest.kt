package com.mowtiie.mesgard.ui.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JumpHighlightTest {
    private val highlight = JumpHighlight()

    @Test
    fun `nothing is highlighted at first`() {
        assertNull(highlight.messageId)
    }

    @Test
    fun `showing a message highlights it, and a later one replaces it`() {
        highlight.show("m1")
        highlight.show("m2")

        assertEquals("m2", highlight.messageId)
    }

    @Test
    fun `clearing removes the highlight`() {
        highlight.show("m1")

        highlight.clear()

        assertNull(highlight.messageId)
    }
}
