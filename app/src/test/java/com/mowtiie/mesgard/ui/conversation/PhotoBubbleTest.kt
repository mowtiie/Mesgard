package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Attachment
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoBubbleTest {
    private fun attachment(mimeType: String) = Attachment(
        id = mimeType,
        messageId = "m",
        fileName = "file",
        mimeType = mimeType,
        sizeBytes = 1L,
        storagePath = "path",
        localPath = null,
    )

    private fun message(attachments: List<Attachment>, isDeleted: Boolean = false) = Message(
        id = "m",
        conversationId = "c",
        senderId = "s",
        senderName = "Ana",
        body = "",
        createdAtEpochMillis = 0L,
        isEdited = false,
        isDeleted = isDeleted,
        status = MessageStatus.SENT,
        isOwn = true,
        attachments = attachments,
    )

    @Test
    fun `a message with only photos is a photo message`() {
        assertTrue(message(listOf(attachment("image/jpeg"), attachment("image/png"))).isPhotoMessage())
    }

    @Test
    fun `a message with a document among its photos keeps the ordinary layout`() {
        assertFalse(message(listOf(attachment("image/jpeg"), attachment("application/pdf"))).isPhotoMessage())
    }

    @Test
    fun `a message without attachments is not a photo message`() {
        assertFalse(message(emptyList()).isPhotoMessage())
    }

    @Test
    fun `a deleted message is never a photo message`() {
        assertFalse(message(listOf(attachment("image/jpeg")), isDeleted = true).isPhotoMessage())
    }
}
