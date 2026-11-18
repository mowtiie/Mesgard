package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class JumpToMessageTest {
    private fun bubble(id: String) = TimelineItem.Bubble(
        Message(
            id = id,
            conversationId = "c1",
            senderId = "u1",
            senderName = "Ana",
            body = "Hello",
            createdAtEpochMillis = 1_000L,
            isEdited = false,
            isDeleted = false,
            status = MessageStatus.SENT,
            isOwn = false,
        ),
    )

    private val timeline = listOf(bubble("newest"), bubble("older"))

    @Test
    fun `a message that is in the list is scrolled to`() {
        assertEquals(JumpStep.Scroll(1), jumpStep(timeline, "older", false, false, 0))
    }

    @Test
    fun `a message that is not in the list asks for older messages`() {
        assertEquals(JumpStep.LoadOlder, jumpStep(timeline, "ancient", false, false, 0))
    }

    @Test
    fun `a load that is still running is waited for`() {
        assertEquals(JumpStep.Wait, jumpStep(timeline, "ancient", true, false, 3))
    }

    @Test
    fun `the jump gives up at the end of the history or after too many pages`() {
        assertEquals(JumpStep.GiveUp, jumpStep(timeline, "ancient", false, true, 3))
        assertEquals(JumpStep.GiveUp, jumpStep(timeline, "ancient", false, false, MAX_JUMP_LOADS))
    }

    @Test
    fun `the message is found even while a load is still running`() {
        assertEquals(JumpStep.Scroll(0), jumpStep(timeline, "newest", true, false, 0))
    }
}
