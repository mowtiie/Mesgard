package com.mowtiie.mesgard.data.local

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.TEST_SENDER_ID
import com.mowtiie.mesgard.testing.messageEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageDaoTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val messageDao get() = databaseRule.database.messageDao()

    @Test
    fun `observeLatest emits the newest message first`() = runTest {
        messageDao.upsert(messageEntity(id = "older", conversationId = CONVERSATION_ID, createdAt = OLDER))
        messageDao.upsert(messageEntity(id = "newer", conversationId = CONVERSATION_ID, createdAt = NEWER))

        messageDao.observeTimeline(CONVERSATION_ID, PAGE_SIZE).test {
            assertEquals(listOf("newer", "older"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeLatest returns only messages of the requested conversation`() = runTest {
        messageDao.upsert(messageEntity(id = "mine", conversationId = CONVERSATION_ID, createdAt = OLDER))
        messageDao.upsert(messageEntity(id = "other", conversationId = "other-conversation", createdAt = NEWER))

        messageDao.observeTimeline(CONVERSATION_ID, PAGE_SIZE).test {
            assertEquals(listOf("mine"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeLatest respects the page size`() = runTest {
        messageDao.upsert(messageEntity(id = "oldest", conversationId = CONVERSATION_ID, createdAt = OLDER))
        messageDao.upsert(messageEntity(id = "newest", conversationId = CONVERSATION_ID, createdAt = NEWER))

        messageDao.observeTimeline(CONVERSATION_ID, limit = 1).test {
            assertEquals(listOf("newest"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsert with the same identifier replaces the message instead of duplicating it`() = runTest {
        messageDao.upsert(
            messageEntity(id = "same", conversationId = CONVERSATION_ID, createdAt = OLDER, body = "first"),
        )
        messageDao.upsert(
            messageEntity(id = "same", conversationId = CONVERSATION_ID, createdAt = OLDER, body = "second"),
        )

        messageDao.observeTimeline(CONVERSATION_ID, PAGE_SIZE).test {
            assertEquals(listOf("second"), awaitItem().map { it.body })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateStatus marks a pending message as sent`() = runTest {
        val pending = messageEntity("pending", CONVERSATION_ID, OLDER, status = MessageStatus.PENDING)
        messageDao.upsert(pending)

        messageDao.updateStatus("pending", MessageStatus.SENT)

        messageDao.observeTimeline(CONVERSATION_ID, PAGE_SIZE).test {
            assertEquals(listOf(MessageStatus.SENT), awaitItem().map { it.status })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteOutsideConversations removes messages of conversations that are no longer kept`() = runTest {
        messageDao.upsertAll(
            listOf(
                messageEntity(id = "kept", conversationId = CONVERSATION_ID, createdAt = OLDER),
                messageEntity(id = "removed", conversationId = "gone", createdAt = OLDER),
            ),
        )

        messageDao.deleteOutsideConversations(listOf(CONVERSATION_ID))

        messageDao.observeTimeline("gone", PAGE_SIZE).test {
            assertEquals(emptyList<String>(), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `timeline includes the display name of the sender when the profile is known`() = runTest {
        databaseRule.database.profileDao().upsertAll(listOf(ProfileEntity(id = TEST_SENDER_ID, displayName = "Ana")))
        messageDao.upsert(messageEntity(id = "m", conversationId = CONVERSATION_ID, createdAt = OLDER))

        messageDao.observeTimeline(CONVERSATION_ID, PAGE_SIZE).test {
            assertEquals("Ana", awaitItem().single().senderName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `timeline keeps a message whose sender profile is unknown`() = runTest {
        messageDao.upsert(messageEntity(id = "m", conversationId = CONVERSATION_ID, createdAt = OLDER))

        messageDao.observeTimeline(CONVERSATION_ID, PAGE_SIZE).test {
            assertNull(awaitItem().single().senderName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `findOldestSentCreatedAt ignores messages that are not yet sent`() = runTest {
        messageDao.upsertAll(
            listOf(
                messageEntity("sent", CONVERSATION_ID, NEWER),
                messageEntity("pending", CONVERSATION_ID, OLDER, status = MessageStatus.PENDING),
            ),
        )

        assertEquals(NEWER, messageDao.findOldestSentCreatedAt(CONVERSATION_ID))
    }

    @Test
    fun `findNewestIncomingId skips the messages of the current user`() = runTest {
        messageDao.upsertAll(
            listOf(
                messageEntity("theirs", CONVERSATION_ID, OLDER).copy(senderId = "other"),
                messageEntity("mine", CONVERSATION_ID, NEWER).copy(senderId = "me"),
            ),
        )

        assertEquals("theirs", messageDao.findNewestIncomingId(CONVERSATION_ID, currentUserId = "me"))
    }

    @Test
    fun `markSent stores the server timestamp and the sent status`() = runTest {
        messageDao.upsert(messageEntity("m", CONVERSATION_ID, OLDER, status = MessageStatus.PENDING))

        messageDao.markSent("m", createdAtEpochMillis = NEWER)

        val stored = messageDao.findById("m")
        assertEquals(MessageStatus.SENT, stored?.status)
        assertEquals(NEWER, stored?.createdAtEpochMillis)
    }

    private companion object {
        const val CONVERSATION_ID = "conversation"
        const val OLDER = 1_000L
        const val NEWER = 2_000L
        const val PAGE_SIZE = 50
    }
}
