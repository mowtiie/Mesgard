package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplyViewModelTest {
    private val viewModel = ReplyViewModel()

    private fun message(body: String) = Message(
        id = "m1",
        conversationId = "c1",
        senderId = "u1",
        senderName = "Ana",
        body = body,
        createdAtEpochMillis = 1_000L,
        isEdited = false,
        isDeleted = false,
        status = MessageStatus.SENT,
        isOwn = false,
    )

    @Test
    fun `replying keeps who wrote the message and the start of its text`() {
        viewModel.onReplyRequested(message("x".repeat(500)))

        val target = viewModel.target.value
        assertEquals("m1", target?.messageId)
        assertEquals("Ana", target?.senderName)
        assertEquals(120, target?.text?.length)
    }

    @Test
    fun `a reply that is dropped is gone`() {
        viewModel.onReplyRequested(message("Hello"))

        viewModel.onReplyDropped()

        assertNull(viewModel.target.value)
    }

    @Test
    fun `a new reply replaces the earlier one`() {
        viewModel.onReplyRequested(message("First"))
        viewModel.onReplyRequested(message("Second").copy(id = "m2"))

        assertEquals("m2", viewModel.target.value?.messageId)
    }
}
