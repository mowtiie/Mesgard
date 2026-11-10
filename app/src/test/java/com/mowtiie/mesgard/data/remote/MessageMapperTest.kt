package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.testing.messageDto
import com.mowtiie.mesgard.testing.messageEntity
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageMapperTest {
    @Test
    fun `maps a fetched message as a sent message with its server timestamp`() {
        val entity = messageDto("m", "chat", createdAt = "2026-11-10T08:00:00.250000+00:00").toEntity()

        assertEquals(MessageStatus.SENT, entity.status)
        assertEquals(Instant.parse("2026-11-10T08:00:00.250Z").toEpochMilli(), entity.createdAtEpochMillis)
    }

    @Test
    fun `maps the deletion timestamp of a deleted message`() {
        val entity = messageDto("m", "chat", deletedAt = "2026-11-10T09:00:00+00:00").toEntity()

        assertEquals(Instant.parse("2026-11-10T09:00:00Z").toEpochMilli(), entity.deletedAtEpochMillis)
        assertNull(entity.editedAtEpochMillis)
    }

    @Test
    fun `extracts the sender profile when it is embedded`() {
        val profile = messageDto("m", "chat", senderId = "user", senderName = "Ana").toSenderProfile()

        assertEquals("Ana", profile?.displayName)
        assertEquals("user", profile?.id)
    }

    @Test
    fun `extracts no sender profile when it is not embedded`() {
        assertNull(messageDto("m", "chat", senderName = null).toSenderProfile())
    }

    @Test
    fun `builds the insert payload from a local message and its organization`() {
        val dto = messageEntity("m", "chat", createdAt = 1_000L, body = "Hello").toNewMessageDto(orgId = "org-1")

        assertEquals(
            NewMessageDto(id = "m", orgId = "org-1", conversationId = "chat", senderId = "sender", body = "Hello"),
            dto,
        )
    }
}
