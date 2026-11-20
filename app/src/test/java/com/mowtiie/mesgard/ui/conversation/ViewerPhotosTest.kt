package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Attachment
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ViewerPhotosTest {
    private fun attachment(id: String, mimeType: String = "image/jpeg", localPath: String? = "/files/$id") = Attachment(
        id = id,
        messageId = "m",
        fileName = id,
        mimeType = mimeType,
        sizeBytes = 1L,
        storagePath = "path",
        localPath = localPath,
    )

    private fun message(vararg attachments: Attachment) = Message(
        id = "m",
        conversationId = "c",
        senderId = "s",
        senderName = "Ana",
        body = "",
        createdAtEpochMillis = 0L,
        isEdited = false,
        isDeleted = false,
        status = MessageStatus.SENT,
        isOwn = false,
        attachments = attachments.toList(),
    )

    @Test
    fun `the viewer starts at the photo that was opened`() {
        val viewed = viewerPhotosOf(message(attachment("a"), attachment("b"), attachment("c")), "b")

        assertEquals(listOf("a", "b", "c"), viewed?.photos?.map { it.id })
        assertEquals(1, viewed?.startIndex)
    }

    @Test
    fun `documents and photos that are not downloaded are left out`() {
        val pending = attachment("b", localPath = null)
        val document = attachment("doc", "application/pdf")
        val viewed = viewerPhotosOf(message(attachment("a"), document, pending, attachment("c")), "c")

        assertEquals(listOf("a", "c"), viewed?.photos?.map { it.id })
        assertEquals(1, viewed?.startIndex)
    }

    @Test
    fun `a photo that is not on the device cannot be opened`() {
        assertNull(viewerPhotosOf(message(attachment("a", localPath = null)), "a"))
    }
}
