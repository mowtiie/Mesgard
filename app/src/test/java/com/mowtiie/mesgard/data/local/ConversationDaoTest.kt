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
    private val preferencesDao get() = databaseRule.database.conversationPreferencesDao()
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
    fun `lists group conversations like any other`() = runTest {
        conversationDao.upsertAll(
            listOf(
                conversationEntity("announcements", type = ConversationType.GROUP),
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
            assertEquals(listOf("recent", "announcements"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lists pinned conversations first, then the rest by activity`() = runTest {
        conversationDao.upsertAll(
            listOf(
                conversationEntity("quiet", pinnedAt = OLDER),
                conversationEntity("active"),
                conversationEntity("pinned-later", pinnedAt = NEWER),
            ),
        )
        messageDao.upsertAll(listOf(messageEntity(id = "m1", conversationId = "active", createdAt = NEWER)))

        conversationDao.observeList(PAGE_SIZE).test {
            assertEquals(listOf("pinned-later", "quiet", "active"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `archived conversations leave the list and have their own`() = runTest {
        conversationDao.upsertAll(listOf(conversationEntity("open"), conversationEntity("shelved", archivedAt = OLDER)))

        conversationDao.observeList(PAGE_SIZE).test {
            assertEquals(listOf("open"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
        conversationDao.observeArchived(PAGE_SIZE).test {
            assertEquals(listOf("shelved"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a conversation deleted for me stays away until a newer message arrives`() = runTest {
        conversationDao.upsertAll(listOf(conversationEntity("chat")))
        messageDao.upsertAll(listOf(messageEntity(id = "m1", conversationId = "chat", createdAt = OLDER)))
        preferencesDao.hide("chat", hiddenBefore = OLDER)

        conversationDao.observeList(PAGE_SIZE).test {
            assertEquals(emptyList<String>(), awaitItem().map { it.id })
            messageDao.upsertAll(listOf(messageEntity(id = "m2", conversationId = "chat", createdAt = NEWER)))
            assertEquals(listOf("chat"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
        messageDao.observeTimeline("chat", PAGE_SIZE).test {
            assertEquals(listOf("m2"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hiding a conversation also unpins, unarchives and clears what was unread`() = runTest {
        conversationDao.upsertAll(
            listOf(conversationEntity("chat", pinnedAt = OLDER, archivedAt = OLDER, unreadCount = 3)),
        )

        preferencesDao.hide("chat", hiddenBefore = NEWER)

        val row = preferencesDao.find(listOf("chat")).single()
        assertNull(row.pinnedAt)
        assertNull(row.archivedAt)
        assertEquals(0, row.unreadCount)
        assertEquals(NEWER, row.hiddenBefore)
    }

    @Test
    fun `restoring puts the earlier preferences back`() = runTest {
        conversationDao.upsertAll(listOf(conversationEntity("chat", pinnedAt = OLDER, unreadCount = 2)))
        val before = preferencesDao.find(listOf("chat")).single()
        preferencesDao.hide("chat", hiddenBefore = NEWER)

        preferencesDao.restore(before)

        assertEquals(before, preferencesDao.find(listOf("chat")).single())
    }

    @Test
    fun `counts the pinned conversations outside a selection`() = runTest {
        conversationDao.upsertAll(
            listOf(
                conversationEntity("a", pinnedAt = OLDER),
                conversationEntity("b", pinnedAt = OLDER),
                conversationEntity("c"),
            ),
        )

        assertEquals(1, preferencesDao.countPinnedExcept(listOf("a", "c")))
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

    @Test
    fun `observeById emits the conversation with its last message`() = runTest {
        conversationDao.upsertAll(listOf(conversationEntity("chat", title = "Study group")))
        messageDao.upsert(messageEntity(id = "m", conversationId = "chat", createdAt = OLDER, body = "Hi"))

        conversationDao.observeById("chat").test {
            val item = awaitItem()
            assertEquals("Study group", item?.title)
            assertEquals("Hi", item?.lastMessageBody)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeById emits null for an unknown conversation`() = runTest {
        conversationDao.observeById("missing").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearUnreadCount resets the unread count of one conversation`() = runTest {
        conversationDao.upsertAll(
            listOf(conversationEntity("read", unreadCount = 4), conversationEntity("other", unreadCount = 2)),
        )

        conversationDao.clearUnreadCount("read")

        conversationDao.observeList(PAGE_SIZE).test {
            assertEquals(mapOf("read" to 0, "other" to 2), awaitItem().associate { it.id to it.unreadCount })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `findOrgId returns the organization of a conversation`() = runTest {
        conversationDao.upsertAll(listOf(conversationEntity("chat")))

        assertEquals("org", conversationDao.findOrgId("chat"))
    }

    private companion object {
        const val OLDER = 1_000L
        const val NEWER = 2_000L
        const val PAGE_SIZE = 50
    }
}
