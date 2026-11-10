package com.mowtiie.mesgard.data.local

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationDaoTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val conversationDao get() = databaseRule.database.conversationDao()
    private val messageDao get() = databaseRule.database.messageDao()

    @Test
    fun `lists the conversation with the newest message first`() = runTest {
        conversationDao.upsertAll(listOf(conversationEntity("quiet"), conversationEntity("active")))
        messageDao.upsertAll(
            listOf(
                messageEntity(id = "m1", conversationId = "quiet", createdAt = OLDER),
                messageEntity(id = "m2", conversationId = "active", createdAt = NEWER),
            ),
        )

        conversationDao.observeList(PAGE_SIZE).test {
            assertEquals(listOf("active", "quiet"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pins announcement conversations above more recent conversations`() = runTest {
        conversationDao.upsertAll(
            listOf(
                conversationEntity("announcements", type = ConversationType.ANNOUNCEMENT),
                conversationEntity("recent"),
            ),
        )
        messageDao.upsertAll(
            listOf(
                messageEntity(id = "m1", conversationId = "announcements", createdAt = OLDER),
                messageEntity(id = "m2", conversationId = "recent", createdAt = NEWER),
            ),
        )

        conversationDao.observeList(PAGE_SIZE).test {
            assertEquals(listOf("announcements", "recent"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `previews the newest message of each conversation`() = runTest {
        conversationDao.upsertAll(listOf(conversationEntity("chat")))
        messageDao.upsertAll(
            listOf(
                messageEntity(id = "m1", conversationId = "chat", createdAt = OLDER, body = "Earlier"),
                messageEntity(id = "m2", conversationId = "chat", createdAt = NEWER, body = "Latest"),
            ),
        )

        conversationDao.observeList(PAGE_SIZE).test {
            assertEquals("Latest", awaitItem().single().lastMessageBody)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `orders a conversation without messages by its creation time`() = runTest {
        conversationDao.upsertAll(
            listOf(
                conversationEntity("old", createdAt = OLDER),
                conversationEntity("new", createdAt = NEWER),
            ),
        )

        conversationDao.observeList(PAGE_SIZE).test {
            val items = awaitItem()
            assertEquals(listOf("new", "old"), items.map { it.id })
            assertNull(items.first().lastMessageBody)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `respects the page size`() = runTest {
        conversationDao.upsertAll(
            listOf(conversationEntity("a", createdAt = OLDER), conversationEntity("b", createdAt = NEWER)),
        )

        conversationDao.observeList(limit = 1).test {
            assertEquals(listOf("b"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsert replaces the unread count of an existing conversation`() = runTest {
        conversationDao.upsertAll(listOf(conversationEntity("chat", unreadCount = 1)))
        conversationDao.upsertAll(listOf(conversationEntity("chat", unreadCount = 5)))

        conversationDao.observeList(PAGE_SIZE).test {
            assertEquals(5, awaitItem().single().unreadCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteAllExcept removes conversations that are not kept`() = runTest {
        conversationDao.upsertAll(listOf(conversationEntity("kept"), conversationEntity("removed")))

        conversationDao.deleteAllExcept(listOf("kept"))

        conversationDao.observeList(PAGE_SIZE).test {
            assertEquals(listOf("kept"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val OLDER = 1_000L
        const val NEWER = 2_000L
        const val PAGE_SIZE = 50
    }
}
