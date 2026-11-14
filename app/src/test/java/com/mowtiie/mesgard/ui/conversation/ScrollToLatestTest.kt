package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollToLatestTest {
    private fun message(id: String, isOwn: Boolean = false) = Message(
        id = id,
        conversationId = "c1",
        senderId = "s1",
        senderName = "Ana",
        body = "Body of $id",
        createdAtEpochMillis = 0L,
        isEdited = false,
        isDeleted = false,
        status = MessageStatus.SENT,
        isOwn = isOwn,
    )

    @Test
    fun `counts incoming messages newer than the last seen one`() {
        val messages = listOf(message("m3"), message("m2"), message("m1"))

        assertEquals(2, unseenIncomingCount(messages, lastSeenId = "m1"))
    }

    @Test
    fun `does not count own messages`() {
        val messages = listOf(message("m3", isOwn = true), message("m2"), message("m1"))

        assertEquals(1, unseenIncomingCount(messages, lastSeenId = "m1"))
    }

    @Test
    fun `counts nothing when the newest message was seen`() {
        val messages = listOf(message("m2"), message("m1"))

        assertEquals(0, unseenIncomingCount(messages, lastSeenId = "m2"))
    }

    @Test
    fun `counts nothing when the last seen message is unknown`() {
        val messages = listOf(message("m2"), message("m1"))

        assertEquals(0, unseenIncomingCount(messages, lastSeenId = null))
        assertEquals(0, unseenIncomingCount(messages, lastSeenId = "missing"))
    }
}
