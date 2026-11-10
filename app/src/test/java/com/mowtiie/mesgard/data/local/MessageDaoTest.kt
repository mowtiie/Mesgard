package com.mowtiie.mesgard.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.MessageStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageDaoTest {
    private lateinit var database: MesgardDatabase
    private lateinit var messageDao: MessageDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MesgardDatabase::class.java,
        ).allowMainThreadQueries().build()
        messageDao = database.messageDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `observeLatest emits the newest message first`() = runTest {
        messageDao.upsert(message(id = "older", createdAt = OLDER))
        messageDao.upsert(message(id = "newer", createdAt = NEWER))

        messageDao.observeLatest(CONVERSATION_ID, PAGE_SIZE).test {
            assertEquals(listOf("newer", "older"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeLatest returns only messages of the requested conversation`() = runTest {
        messageDao.upsert(message(id = "mine", createdAt = OLDER))
        messageDao.upsert(message(id = "other", createdAt = NEWER, conversationId = "other-conversation"))

        messageDao.observeLatest(CONVERSATION_ID, PAGE_SIZE).test {
            assertEquals(listOf("mine"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeLatest respects the page size`() = runTest {
        messageDao.upsert(message(id = "oldest", createdAt = OLDER))
        messageDao.upsert(message(id = "newest", createdAt = NEWER))

        messageDao.observeLatest(CONVERSATION_ID, limit = 1).test {
            assertEquals(listOf("newest"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsert with the same identifier replaces the message instead of duplicating it`() = runTest {
        messageDao.upsert(message(id = "same", createdAt = OLDER, body = "first"))
        messageDao.upsert(message(id = "same", createdAt = OLDER, body = "second"))

        messageDao.observeLatest(CONVERSATION_ID, PAGE_SIZE).test {
            assertEquals(listOf("second"), awaitItem().map { it.body })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateStatus marks a pending message as sent`() = runTest {
        messageDao.upsert(message(id = "pending", createdAt = OLDER, status = MessageStatus.PENDING))

        messageDao.updateStatus("pending", MessageStatus.SENT)

        messageDao.observeLatest(CONVERSATION_ID, PAGE_SIZE).test {
            assertEquals(listOf(MessageStatus.SENT), awaitItem().map { it.status })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun message(
        id: String,
        createdAt: Long,
        conversationId: String = CONVERSATION_ID,
        body: String = "Hello",
        status: MessageStatus = MessageStatus.SENT,
    ) = MessageEntity(
        id = id,
        conversationId = conversationId,
        senderId = SENDER_ID,
        body = body,
        createdAtEpochMillis = createdAt,
        status = status,
    )

    private companion object {
        const val CONVERSATION_ID = "conversation"
        const val SENDER_ID = "sender"
        const val OLDER = 1_000L
        const val NEWER = 2_000L
        const val PAGE_SIZE = 50
    }
}
