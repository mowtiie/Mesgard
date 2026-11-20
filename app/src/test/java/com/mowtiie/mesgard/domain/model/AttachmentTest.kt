package com.mowtiie.mesgard.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentTest {
    private fun attachment(mimeType: String) = Attachment(
        id = "a",
        messageId = "m",
        fileName = "file",
        mimeType = mimeType,
        sizeBytes = 1L,
        storagePath = "path",
        localPath = null,
    )

    @Test
    fun `a gif is an image and is recognised as a gif`() {
        val gif = attachment("image/gif")

        assertTrue(gif.isImage)
        assertTrue(gif.isGif)
    }

    @Test
    fun `other photos are not gifs`() {
        assertFalse(attachment("image/png").isGif)
        assertFalse(attachment("image/webp").isGif)
    }
}
