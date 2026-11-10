package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.testing.summaryDto
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationMapperTest {
    @Test
    fun `maps the conversation type regardless of case`() {
        assertEquals(ConversationType.ANNOUNCEMENT, summaryDto("a", type = "announcement").toEntity().type)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an unknown conversation type`() {
        summaryDto("a", type = "channel").toEntity()
    }

    @Test
    fun `parses a timestamp with fractional seconds and an offset`() {
        val dto = summaryDto("a").copy(createdAt = "2026-11-10T08:00:00.123456+08:00")

        assertEquals(epochMillisOf("2026-11-10T00:00:00.123Z"), dto.toEntity().createdAtEpochMillis)
    }

    @Test
    fun `maps the last message as a sent message of the conversation`() {
        val message = summaryDto("a").toLastMessageEntity()

        assertEquals("message-a", message?.id)
        assertEquals("a", message?.conversationId)
        assertEquals(MessageStatus.SENT, message?.status)
    }

    @Test
    fun `maps no last message for a conversation without messages`() {
        assertNull(summaryDto("a", lastMessageBody = null, lastMessageCreatedAt = null).toLastMessageEntity())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a last message that is missing its body`() {
        summaryDto("a").copy(lastMessageBody = null).toLastMessageEntity()
    }

    private fun epochMillisOf(timestamp: String): Long = Instant.parse(timestamp).toEpochMilli()
}
