package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.attachment.AttachmentIntake
import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.data.local.ProfileEntity
import com.mowtiie.mesgard.data.local.upsert
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.data.work.MessageSendScheduler
import com.mowtiie.mesgard.domain.attachment.AttachmentImporter
import com.mowtiie.mesgard.domain.attachment.AttachmentRejection
import com.mowtiie.mesgard.domain.attachment.ImportResult
import com.mowtiie.mesgard.domain.attachment.MAX_ATTACHMENTS_PER_MESSAGE
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.domain.model.PageResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.ReplyPreview
import com.mowtiie.mesgard.domain.model.SendRejection
import com.mowtiie.mesgard.domain.model.SendResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MAX_MESSAGE_LENGTH
import com.mowtiie.mesgard.domain.repository.MESSAGE_PAGE_SIZE
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.attachmentDto
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageDto
import com.mowtiie.mesgard.testing.messageEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstMessageRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remoteSource = mockk<MessageRemoteSource>()
    private val sendScheduler = mockk<MessageSendScheduler>(relaxed = true)
    private val attachmentImporter = mockk<AttachmentImporter>(relaxed = true)
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns CURRENT_USER_ID }
    private val clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    private val repository by lazy {
        OfflineFirstMessageRepository(
            databaseRule.database,
            remoteSource,
            sendScheduler,
            AttachmentIntake(attachmentImporter),
            authRepository,
            clock,
        )
    }

    @Before
    fun storeConversation() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity(CONVERSATION_ID)))
    }

    @Test
    fun `send stores a pending message from the current user and schedules its delivery`() = runTest {
        val result = repository.send(CONVERSATION_ID, "  Hello  ")

        assertEquals(SendResult.Queued, result)
        val stored = pendingMessages().single()
        assertEquals("Hello", stored.body)
        assertEquals(CURRENT_USER_ID, stored.senderId)
        assertEquals(NOW, stored.createdAtEpochMillis)
        verify(exactly = 1) { sendScheduler.schedule(stored.id) }
    }

    @Test
    fun `send stores a pending message with its attachments and schedules it`() = runTest {
        coEvery { attachmentImporter.import("content://photo", any()) } returns
            ImportResult.Imported("photo.jpg", "image/jpeg", 1_024L, "/files/photo")

        val result = repository.send(CONVERSATION_ID, "", listOf("content://photo"))

        assertEquals(SendResult.Queued, result)
        val message = pendingMessages().single()
        val attachment = databaseRule.database.attachmentDao().findByMessage(message.id).single()
        assertEquals("photo.jpg", attachment.fileName)
        assertEquals("/files/photo", attachment.localPath)
        assertEquals("$CONVERSATION_ID/${message.id}/${attachment.id}", attachment.storagePath)
        verify(exactly = 1) { sendScheduler.schedule(message.id) }
    }

    @Test
    fun `send stores the message it answers`() = runTest {
        repository.send(CONVERSATION_ID, "An answer", replyToId = "original")

        assertEquals("original", pendingMessages().single().replyToId)
    }

    @Test
    fun `send stores who the message mentions`() = runTest {
        repository.send(CONVERSATION_ID, "Hello @Ana", mentionedUserIds = listOf("ana", "ben"))

        assertEquals("ana,ben", pendingMessages().single().mentionedUserIds)
    }

    @Test
    fun `a message shows the names of the people it mentions and whether it mentions you`() = runTest {
        databaseRule.database.profileDao().upsertAll(listOf(ProfileEntity("ana", "Ana Cruz")))
        databaseRule.database.messageDao().upsert(
            messageEntity(
                "m1",
                CONVERSATION_ID,
                1_000L,
            ).copy(body = "@Ana Cruz @me", mentionedUserIds = "ana,$CURRENT_USER_ID"),
        )

        repository.observeMessages(CONVERSATION_ID, PAGE).test {
            val message = awaitItem().single()
            assertEquals(listOf("Ana Cruz"), message.mentionNames)
            assertEquals(true, message.mentionsYou)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a reply shows what the original said and who wrote it`() = runTest {
        databaseRule.database.profileDao().upsertAll(listOf(ProfileEntity("ana", "Ana")))
        databaseRule.database.messageDao().upsert(
            messageEntity("original", CONVERSATION_ID, 1_000L).copy(senderId = "ana", body = "Is it due Friday?"),
        )
        databaseRule.database.messageDao().upsert(
            messageEntity("answer", CONVERSATION_ID, 2_000L).copy(replyToId = "original"),
        )

        repository.observeMessages(CONVERSATION_ID, PAGE).test {
            val reply = awaitItem().first { it.id == "answer" }.replyTo
            assertEquals(
                ReplyPreview(
                    "original",
                    isLoaded = true,
                    senderName = "Ana",
                    body = "Is it due Friday?",
                    isDeleted = false,
                ),
                reply,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a reply to a message that is not on the device says so`() = runTest {
        databaseRule.database.messageDao().upsert(
            messageEntity("answer", CONVERSATION_ID, 2_000L).copy(replyToId = "older"),
        )

        repository.observeMessages(CONVERSATION_ID, PAGE).test {
            val reply = awaitItem().single().replyTo
            assertEquals(false, reply?.isLoaded)
            assertEquals("older", reply?.messageId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a reply to a deleted message marks it deleted`() = runTest {
        databaseRule.database.messageDao().upsert(
            messageEntity("original", CONVERSATION_ID, 1_000L).copy(body = "", deletedAtEpochMillis = 1_500L),
        )
        databaseRule.database.messageDao().upsert(
            messageEntity("answer", CONVERSATION_ID, 2_000L).copy(replyToId = "original"),
        )

        repository.observeMessages(CONVERSATION_ID, PAGE).test {
            assertEquals(true, awaitItem().first { it.id == "answer" }.replyTo?.isDeleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `send rejects more attachments than a message can carry`() = runTest {
        val uris = List(MAX_ATTACHMENTS_PER_MESSAGE + 1) { "content://file$it" }

        assertEquals(
            SendResult.Rejected(SendRejection.TOO_MANY_ATTACHMENTS),
            repository.send(CONVERSATION_ID, "Hello", uris),
        )
    }

    @Test
    fun `send stores nothing and removes imported files when an attachment is rejected`() = runTest {
        coEvery { attachmentImporter.import("content://first", any()) } returns
            ImportResult.Imported("first.pdf", "application/pdf", 10L, "/files/first")
        coEvery { attachmentImporter.import("content://second", any()) } returns
            ImportResult.Rejected(AttachmentRejection.TOO_LARGE)

        val result = repository.send(CONVERSATION_ID, "Hello", listOf("content://first", "content://second"))

        assertEquals(SendResult.Rejected(SendRejection.ATTACHMENT_TOO_LARGE), result)
        assertTrue(pendingMessages().isEmpty())
        verify { attachmentImporter.delete("/files/first") }
        verify(exactly = 0) { sendScheduler.schedule(any()) }
    }

    @Test
    fun `send rejects an empty message`() = runTest {
        assertEquals(SendResult.Rejected(SendRejection.EMPTY_MESSAGE), repository.send(CONVERSATION_ID, "   "))
        verify(exactly = 0) { sendScheduler.schedule(any()) }
    }

    @Test
    fun `send rejects a message longer than the limit`() = runTest {
        val tooLong = "a".repeat(MAX_MESSAGE_LENGTH + 1)

        assertEquals(SendResult.Rejected(SendRejection.MESSAGE_TOO_LONG), repository.send(CONVERSATION_ID, tooLong))
    }

    @Test
    fun `send rejects a message when nobody is signed in`() = runTest {
        every { authRepository.currentUserId } returns null

        assertEquals(SendResult.Rejected(SendRejection.NOT_SIGNED_IN), repository.send(CONVERSATION_ID, "Hello"))
    }

    @Test
    fun `send rejects a message for a conversation that is not stored`() = runTest {
        val result = repository.send("unknown", "Hello")

        assertEquals(SendResult.Rejected(SendRejection.CONVERSATION_UNAVAILABLE), result)
        verify(exactly = 0) { sendScheduler.schedule(any()) }
    }

    @Test
    fun `retry marks a failed message as pending and schedules it again`() = runTest {
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("failed", CONVERSATION_ID, 1_000L, status = MessageStatus.FAILED))

        repository.retry("failed")

        assertEquals(MessageStatus.PENDING, dao.findById("failed")?.status)
        verify(exactly = 1) { sendScheduler.schedule("failed") }
    }

    @Test
    fun `discard removes a failed message with its attachment files`() = runTest {
        val file = File.createTempFile("attachment", ".bin")
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("failed", CONVERSATION_ID, 1_000L, status = MessageStatus.FAILED))
        databaseRule.database.attachmentDao().upsertAll(
            listOf(storedAttachment("failed", file.absolutePath)),
        )

        repository.discard("failed")

        assertEquals(null, dao.findById("failed"))
        assertTrue(databaseRule.database.attachmentDao().findByMessage("failed").isEmpty())
        assertFalse(file.exists())
    }

    @Test
    fun `discard keeps a message that is not failed`() = runTest {
        val file = File.createTempFile("attachment", ".bin")
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("pending", CONVERSATION_ID, 1_000L, status = MessageStatus.PENDING))
        databaseRule.database.attachmentDao().upsertAll(
            listOf(storedAttachment("pending", file.absolutePath)),
        )

        repository.discard("pending")

        assertEquals(MessageStatus.PENDING, dao.findById("pending")?.status)
        assertTrue(file.exists())
        file.delete()
    }

    private fun storedAttachment(messageId: String, localPath: String) = AttachmentEntity(
        id = "a-$messageId",
        messageId = messageId,
        storagePath = "path/$messageId",
        mimeType = "image/png",
        sizeBytes = 1L,
        fileName = "photo.png",
        localPath = localPath,
    )

    @Test
    fun `refresh stores the attachments of a message and shows them in the timeline`() = runTest {
        coEvery { remoteSource.fetchLatest(CONVERSATION_ID) } returns
            listOf(messageDto("m1", CONVERSATION_ID, attachments = listOf(attachmentDto("a1", "photo.png"))))

        repository.refresh(CONVERSATION_ID)

        val message = repository.observeMessages(CONVERSATION_ID, MESSAGE_PAGE_SIZE).first().single()
        assertEquals(listOf("photo.png"), message.attachments.map { it.fileName })
        assertTrue(message.attachments.single().isImage)
    }

    @Test
    fun `refresh keeps the local file of an attachment that was already stored`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity("m1", CONVERSATION_ID, 1_000L))
        databaseRule.database.attachmentDao().upsertAll(
            listOf(attachmentDto("a1").toEntity("m1", localPath = "/files/a1")),
        )
        coEvery { remoteSource.fetchLatest(CONVERSATION_ID) } returns
            listOf(messageDto("m1", CONVERSATION_ID, attachments = listOf(attachmentDto("a1"))))

        repository.refresh(CONVERSATION_ID)

        val message = repository.observeMessages(CONVERSATION_ID, MESSAGE_PAGE_SIZE).first().single()
        assertEquals("/files/a1", message.attachments.single().localPath)
    }

    @Test
    fun `refresh stores the latest messages with their sender names`() = runTest {
        coEvery { remoteSource.fetchLatest(CONVERSATION_ID) } returns
            listOf(messageDto("m1", CONVERSATION_ID, senderName = "Ana"))

        val result = repository.refresh(CONVERSATION_ID)

        assertEquals(RefreshResult.Success, result)
        repository.observeMessages(CONVERSATION_ID, PAGE).test {
            assertEquals("Ana", awaitItem().single().senderName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reports a network failure and keeps the stored messages`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity("stored", CONVERSATION_ID, 1_000L))
        coEvery { remoteSource.fetchLatest(CONVERSATION_ID) } throws IOException("offline")

        val result = repository.refresh(CONVERSATION_ID)

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        repository.observeMessages(CONVERSATION_ID, PAGE).test {
            assertEquals(listOf("stored"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeMessages flags the messages of the current user`() = runTest {
        databaseRule.database.messageDao().upsertAll(
            listOf(
                messageEntity("mine", CONVERSATION_ID, 2_000L).copy(senderId = CURRENT_USER_ID),
                messageEntity("theirs", CONVERSATION_ID, 1_000L),
            ),
        )

        repository.observeMessages(CONVERSATION_ID, PAGE).test {
            assertEquals(mapOf("mine" to true, "theirs" to false), awaitItem().associate { it.id to it.isOwn })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeMessages marks a deleted message`() = runTest {
        databaseRule.database.messageDao().upsert(
            messageEntity("gone", CONVERSATION_ID, 1_000L).copy(deletedAtEpochMillis = 2_000L, body = ""),
        )

        repository.observeMessages(CONVERSATION_ID, PAGE).test {
            assertTrue(awaitItem().single().isDeleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadOlder fetches the page before the oldest sent message and reports the end of history`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity("newest", CONVERSATION_ID, OLDEST_MILLIS))
        coEvery { remoteSource.fetchOlder(CONVERSATION_ID, OLDEST_MILLIS + 1) } returns
            listOf(messageDto("older", CONVERSATION_ID, createdAt = "2026-11-09T08:00:00+00:00"))

        val result = repository.loadOlder(CONVERSATION_ID)

        assertEquals(PageResult.Loaded(endReached = true), result)
        repository.observeMessages(CONVERSATION_ID, PAGE).test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadOlder reports that more history exists when the server returned a full page`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity("newest", CONVERSATION_ID, OLDEST_MILLIS))
        coEvery { remoteSource.fetchOlder(any(), any()) } returns
            List(MESSAGE_PAGE_SIZE) { messageDto("older-$it", CONVERSATION_ID) }

        assertEquals(PageResult.Loaded(endReached = false), repository.loadOlder(CONVERSATION_ID))
    }

    @Test
    fun `loadOlder reports a network failure`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity("newest", CONVERSATION_ID, OLDEST_MILLIS))
        coEvery { remoteSource.fetchOlder(any(), any()) } throws IOException("offline")

        assertEquals(PageResult.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.loadOlder(CONVERSATION_ID))
    }

    @Test
    fun `markConversationRead clears the unread count and queues the read marker`() = runTest {
        databaseRule.database.conversationDao()
            .upsertAll(listOf(conversationEntity(CONVERSATION_ID, unreadCount = 3)))

        repository.markConversationRead(CONVERSATION_ID)

        verify { sendScheduler.scheduleReadMarker(CONVERSATION_ID) }
        databaseRule.database.conversationDao().observeById(CONVERSATION_ID).test {
            assertEquals(0, awaitItem()?.unreadCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeNewestIncomingId follows the newest sent message from someone else`() = runTest {
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("old", CONVERSATION_ID, 1_000L))
        dao.upsert(messageEntity("mine", CONVERSATION_ID, 3_000L).copy(senderId = CURRENT_USER_ID))

        repository.observeNewestIncomingId(CONVERSATION_ID).test {
            assertEquals("old", awaitItem())
            dao.upsert(messageEntity("new", CONVERSATION_ID, 2_000L))
            assertEquals("new", awaitItem())
        }
    }

    @Test
    fun `refresh does not replace a pending message that is not yet on the server`() = runTest {
        databaseRule.database.messageDao()
            .upsert(messageEntity("pending", CONVERSATION_ID, 5_000L, status = MessageStatus.PENDING))
        coEvery { remoteSource.fetchLatest(CONVERSATION_ID) } returns emptyList()

        repository.refresh(CONVERSATION_ID)

        assertEquals(MessageStatus.PENDING, databaseRule.database.messageDao().findById("pending")?.status)
    }

    @Test
    fun `refresh with a full page drops older stored messages so the history stays contiguous`() = runTest {
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("ancient", CONVERSATION_ID, 1_000L))
        dao.upsert(messageEntity("pending-old", CONVERSATION_ID, 1_000L, status = MessageStatus.PENDING))
        coEvery { remoteSource.fetchLatest(CONVERSATION_ID) } returns
            List(MESSAGE_PAGE_SIZE) { messageDto("m-$it", CONVERSATION_ID, createdAt = "2026-11-10T08:00:00+00:00") }

        repository.refresh(CONVERSATION_ID)

        assertEquals(null, dao.findById("ancient"))
        assertEquals(MessageStatus.PENDING, dao.findById("pending-old")?.status)
    }

    @Test
    fun `refresh with a full page that continues the stored history keeps the older messages`() = runTest {
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("ancient", CONVERSATION_ID, 1_000L))
        val sentAt = "2026-11-10T08:00:00+00:00"
        val page = List(MESSAGE_PAGE_SIZE) { messageDto("m-$it", CONVERSATION_ID, createdAt = sentAt) }
        dao.upsert(messageEntity("m-0", CONVERSATION_ID, 1_700_000_000_000L))
        coEvery { remoteSource.fetchLatest(CONVERSATION_ID) } returns page

        repository.refresh(CONVERSATION_ID)

        assertEquals("ancient", dao.findById("ancient")?.id)
    }

    @Test
    fun `refresh with a short page keeps the older stored messages`() = runTest {
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("ancient", CONVERSATION_ID, 1_000L))
        coEvery { remoteSource.fetchLatest(CONVERSATION_ID) } returns listOf(messageDto("m", CONVERSATION_ID))

        repository.refresh(CONVERSATION_ID)

        assertEquals("ancient", dao.findById("ancient")?.id)
    }

    private suspend fun pendingMessages() = databaseRule.database.messageDao()
        .observeTimeline(CONVERSATION_ID, PAGE)
        .first()
        .filter { it.status == MessageStatus.PENDING }

    private companion object {
        const val CONVERSATION_ID = "chat"
        const val CURRENT_USER_ID = "me"
        const val NOW = 1_700_000_000_000L
        const val OLDEST_MILLIS = 1_700_000_100_000L
        const val PAGE = 50
    }
}
