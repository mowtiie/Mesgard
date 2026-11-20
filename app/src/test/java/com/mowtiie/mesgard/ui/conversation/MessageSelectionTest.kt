package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageSelectionTest {
    private fun message(
        id: String,
        isOwn: Boolean = true,
        body: String = "Hello",
        status: MessageStatus = MessageStatus.SENT,
        isDeleted: Boolean = false,
        at: Long = 0L,
    ) = Message(
        id = id,
        conversationId = "c",
        senderId = "s",
        senderName = "Ana",
        body = body,
        createdAtEpochMillis = at,
        isEdited = false,
        isDeleted = isDeleted,
        status = status,
        isOwn = isOwn,
    )

    private fun actions(vararg selected: Message, canPin: Boolean = false, pinned: Set<String> = emptySet()) =
        selectionActionsFor(selected.toList(), canPin, pinned)

    @Test
    fun `one sent message of your own offers everything`() {
        val result = actions(message("m1"), canPin = true)

        assertTrue(result.canReply)
        assertTrue(result.canCopy)
        assertTrue(result.canDelete)
        assertTrue(result.canEdit)
        assertTrue(result.canShowInfo)
        assertEquals(PinChoice.PIN, result.pin)
    }

    @Test
    fun `a message from someone else can be replied to and copied but not changed`() {
        val result = actions(message("m1", isOwn = false))

        assertTrue(result.canReply)
        assertTrue(result.canCopy)
        assertFalse(result.canDelete)
        assertFalse(result.canEdit)
        assertFalse(result.canShowInfo)
    }

    @Test
    fun `reply, edit, info and pin need exactly one message`() {
        val result = actions(message("m1"), message("m2"), canPin = true)

        assertFalse(result.canReply)
        assertFalse(result.canEdit)
        assertFalse(result.canShowInfo)
        assertEquals(PinChoice.NONE, result.pin)
        assertTrue(result.canCopy)
        assertTrue(result.canDelete)
    }

    @Test
    fun `delete is offered only when every selected message is yours and sent`() {
        assertFalse(actions(message("m1"), message("m2", isOwn = false)).canDelete)
        assertFalse(actions(message("m1"), message("m2", status = MessageStatus.PENDING)).canDelete)
        assertTrue(actions(message("m1"), message("m2")).canDelete)
    }

    @Test
    fun `a message that is not sent yet cannot be replied to`() {
        val result = actions(message("m1", status = MessageStatus.PENDING))

        assertFalse(result.canReply)
        assertFalse(result.canDelete)
    }

    @Test
    fun `a deleted message offers nothing`() {
        val result = actions(message("m1", body = "", isDeleted = true), canPin = true)

        assertFalse(result.canCopy)
        assertFalse(result.canReply)
        assertFalse(result.canDelete)
        assertEquals(PinChoice.NONE, result.pin)
    }

    @Test
    fun `a pinned message offers to unpin it, and only staff can pin`() {
        assertEquals(PinChoice.UNPIN, actions(message("m1"), canPin = true, pinned = setOf("m1")).pin)
        assertEquals(PinChoice.NONE, actions(message("m1"), canPin = false).pin)
    }

    @Test
    fun `only a person who may pin is shown the pin item`() {
        assertTrue(actions(message("m1"), canPin = true).canManagePins)
        assertFalse(actions(message("m1"), canPin = false).canManagePins)
    }

    @Test
    fun `a person who may pin keeps the item when it does not apply to the selection`() {
        val result = actions(message("m1"), message("m2"), canPin = true)

        assertTrue(result.canManagePins)
        assertEquals(PinChoice.NONE, result.pin)
    }

    @Test
    fun `a message without text cannot be copied or edited`() {
        val result = actions(message("m1", body = ""))

        assertFalse(result.canCopy)
        assertFalse(result.canEdit)
    }

    @Test
    fun `the copied text is the oldest message first, one per line, without deleted or empty ones`() {
        val copied = copyTextOf(
            listOf(
                message("m2", body = "second", at = 2L),
                message("m1", body = "first", at = 1L),
                message("m3", body = "", at = 3L),
                message("m4", body = "gone", at = 4L, isDeleted = true),
            ),
        )

        assertEquals("first\nsecond", copied)
    }
}
