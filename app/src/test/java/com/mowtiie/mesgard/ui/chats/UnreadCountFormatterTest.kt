package com.mowtiie.mesgard.ui.chats

import org.junit.Assert.assertEquals
import org.junit.Test

class UnreadCountFormatterTest {
    @Test
    fun `shows the exact count up to 999`() {
        assertEquals("1", formatUnreadCount(1))
        assertEquals("999", formatUnreadCount(999))
    }

    @Test
    fun `caps larger counts at 999 with a plus sign so the badge stays within four characters`() {
        assertEquals("999+", formatUnreadCount(1_000))
        assertEquals("999+", formatUnreadCount(25_000))
    }
}
