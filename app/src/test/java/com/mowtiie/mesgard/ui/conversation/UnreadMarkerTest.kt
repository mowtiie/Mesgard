package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class UnreadMarkerTest {
    private fun message(id: String, isOwn: Boolean = false) = Message(
        id = id,
        conversationId = "c1",
        senderId = if (isOwn) "me" else "other",
        senderName = "Ana",
        body = "Body of $id",
        createdAtEpochMillis = 0L,
        isEdited = false,
        isDeleted = false,
        status = MessageStatus.SENT,
        isOwn = isOwn,
    )

    @Test
    fun `the first unread message is the oldest of the unread ones from other people`() {
        val newestFirst = listOf(message("m4"), message("m3", isOwn = true), message("m2"), message("m1"))

        assertEquals("m2", firstUnreadMessageId(newestFirst, unreadCount = 2))
    }

    @Test
    fun `falls back to the oldest loaded message when fewer are loaded than unread`() {
        val newestFirst = listOf(message("m2"), message("m1"))

        assertEquals("m1", firstUnreadMessageId(newestFirst, unreadCount = 40))
    }

    @Test
    fun `there is no first unread message when nothing is unread or nothing is incoming`() {
        assertEquals(null, firstUnreadMessageId(listOf(message("m1")), unreadCount = 0))
        assertEquals(null, firstUnreadMessageId(listOf(message("m1", isOwn = true)), unreadCount = 3))
    }
}
