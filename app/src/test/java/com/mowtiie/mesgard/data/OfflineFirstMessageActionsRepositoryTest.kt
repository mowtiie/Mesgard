package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.data.local.upsert
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.ReaderProfileDto
import com.mowtiie.mesgard.data.remote.ReceiptDto
import com.mowtiie.mesgard.domain.model.ReadersResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MESSAGE_READERS_LIMIT
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageDto
import com.mowtiie.mesgard.testing.messageEntity
import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val CONVERSATION_ID = "c1"

@RunWith(RobolectricTestRunner::class)
class OfflineFirstMessageActionsRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remoteSource = mockk<MessageRemoteSource>()
    private val repository by lazy { OfflineFirstMessageActionsRepository(databaseRule.database, remoteSource) }

    @Before
    fun storeConversation() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity(CONVERSATION_ID)))
    }

    private fun storedAttachment(localPath: String) = AttachmentEntity(
        id = "a-mine",
        messageId = "mine",
        storagePath = "path/mine",
        mimeType = "image/png",
        sizeBytes = 1L,
        fileName = "mine.png",
        localPath = localPath,
    )

    @Test
    fun `readers lists who read the message with the time`() = runTest {
        coEvery { remoteSource.fetchReaders("mine", MESSAGE_READERS_LIMIT) } returns listOf(
            ReceiptDto("u1", "2026-11-18T08:00:00+00:00", ReaderProfileDto("Ana")),
            ReceiptDto("u2", "2026-11-18T07:00:00+00:00", null),
        )

        val result = repository.readers("mine") as ReadersResult.Loaded

        assertEquals(listOf("Ana", ""), result.readers.map { it.displayName })
        assertEquals(1_794_988_800_000L, result.readers.first().readAtEpochMillis)
    }

    @Test
    fun `readers reports a connection problem`() = runTest {
        coEvery { remoteSource.fetchReaders(any(), any()) } throws IOException("offline")

        assertEquals(ReadersResult.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.readers("mine"))
    }

    @Test
    fun `delete stores the deleted message the server answers with and removes its local files`() = runTest {
        val file = File.createTempFile("attachment", ".bin")
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("mine", CONVERSATION_ID, 1_000L))
        databaseRule.database.attachmentDao().upsertAll(listOf(storedAttachment(file.absolutePath)))
        coEvery { remoteSource.delete("mine") } returns
            messageDto("mine", CONVERSATION_ID, deletedAt = "2026-11-18T08:00:00+00:00")

        val result = repository.delete("mine")

        assertEquals(RefreshResult.Success, result)
        assertNotNull(dao.findById("mine")?.deletedAtEpochMillis)
        assertEquals("", dao.findById("mine")?.body)
        assertFalse(file.exists())
    }

    @Test
    fun `delete keeps the message and its files when the server cannot be reached`() = runTest {
        val file = File.createTempFile("attachment", ".bin")
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("mine", CONVERSATION_ID, 1_000L))
        databaseRule.database.attachmentDao().upsertAll(listOf(storedAttachment(file.absolutePath)))
        coEvery { remoteSource.delete("mine") } throws IOException("offline")

        val result = repository.delete("mine")

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        assertNull(dao.findById("mine")?.deletedAtEpochMillis)
        assertTrue(file.exists())
        file.delete()
    }
}
