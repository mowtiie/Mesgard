package com.mowtiie.mesgard.data.attachment

import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.data.local.upsert
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageEntity
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttachmentCacheTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dao by lazy { databaseRule.database.attachmentDao() }
    private val cache by lazy { AttachmentCache(dao) }

    @Before
    fun storeConversation() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("chat")))
    }

    @Test
    fun `removes the oldest files until the cache fits the limit and keeps the one just downloaded`() = runTest {
        val old = store("old", messageTime = 1_000L)
        val middle = store("middle", messageTime = 2_000L)
        val newest = store("newest", messageTime = 3_000L)

        cache.trim(keepAttachmentId = "old", limitBytes = 20L)

        assertTrue(old.exists())
        assertFalse(middle.exists())
        assertTrue(newest.exists())
        assertEquals(null, dao.findByIds(listOf("middle")).single().localPath)
        assertEquals(newest.absolutePath, dao.findByIds(listOf("newest")).single().localPath)
    }

    @Test
    fun `leaves everything alone while the cache is within the limit`() = runTest {
        val file = store("a", messageTime = 1_000L)

        cache.trim(keepAttachmentId = "other", limitBytes = 100L)

        assertTrue(file.exists())
    }

    @Test
    fun `never removes the file of a message that is not sent yet`() = runTest {
        val pending = store("pending", messageTime = 1_000L, status = MessageStatus.PENDING)

        cache.trim(keepAttachmentId = "other", limitBytes = 0L)

        assertTrue(pending.exists())
    }

    private suspend fun store(id: String, messageTime: Long, status: MessageStatus = MessageStatus.SENT): File {
        val file = temporaryFolder.newFile(id).apply { writeText("0123456789") }
        databaseRule.database.messageDao().upsert(messageEntity("m-$id", "chat", messageTime, status = status))
        dao.upsertAll(
            listOf(
                AttachmentEntity(
                    id = id,
                    messageId = "m-$id",
                    storagePath = "chat/m-$id/$id",
                    mimeType = "application/pdf",
                    sizeBytes = file.length(),
                    fileName = "$id.pdf",
                    localPath = file.absolutePath,
                ),
            ),
        )
        return file
    }
}
