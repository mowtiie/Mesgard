package com.mowtiie.mesgard.data.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.data.remote.AttachmentRemoteSource
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.NewMessageDto
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.TEST_ORG_ID
import com.mowtiie.mesgard.testing.TEST_SENDER_ID
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageDto
import com.mowtiie.mesgard.testing.messageEntity
import io.github.jan.supabase.exceptions.RestException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SendMessageWorkerTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val remoteSource = mockk<MessageRemoteSource>()
    private val attachmentSource = mockk<AttachmentRemoteSource>()

    @Before
    fun storeConversation() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity(CONVERSATION_ID)))
    }

    @Test
    fun `sends a pending message and stores the server timestamp`() = runTest {
        storePending()
        coEvery { remoteSource.send(any()) } returns messageDto(MESSAGE_ID, CONVERSATION_ID, createdAt = SERVER_TIME)

        val result = worker().doWork()

        assertEquals(Result.success(), result)
        val stored = databaseRule.database.messageDao().findById(MESSAGE_ID)
        assertEquals(MessageStatus.SENT, stored?.status)
        assertEquals(Instant.parse(SERVER_TIME).toEpochMilli(), stored?.createdAtEpochMillis)
        coVerify {
            remoteSource.send(NewMessageDto(MESSAGE_ID, TEST_ORG_ID, CONVERSATION_ID, TEST_SENDER_ID, "Hello"))
        }
    }

    @Test
    fun `does not send a message that is already sent`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity(MESSAGE_ID, CONVERSATION_ID, 1_000L))

        val result = worker().doWork()

        assertEquals(Result.success(), result)
        coVerify(exactly = 0) { remoteSource.send(any()) }
    }

    @Test
    fun `retries when the server cannot be reached`() = runTest {
        storePending()
        coEvery { remoteSource.send(any()) } throws IOException("offline")

        val result = worker(runAttemptCount = 0).doWork()

        assertEquals(Result.retry(), result)
        assertEquals(MessageStatus.PENDING, databaseRule.database.messageDao().findById(MESSAGE_ID)?.status)
    }

    @Test
    fun `marks the message as failed after the last attempt`() = runTest {
        storePending()
        coEvery { remoteSource.send(any()) } throws IOException("offline")

        val result = worker(runAttemptCount = LAST_ATTEMPT_INDEX).doWork()

        assertEquals(Result.failure(), result)
        assertEquals(MessageStatus.FAILED, databaseRule.database.messageDao().findById(MESSAGE_ID)?.status)
    }

    @Test
    fun `marks the message as failed when the server rejects it`() = runTest {
        storePending()
        coEvery { remoteSource.send(any()) } throws mockk<RestException>(relaxed = true)

        val result = worker().doWork()

        assertEquals(Result.failure(), result)
        assertEquals(MessageStatus.FAILED, databaseRule.database.messageDao().findById(MESSAGE_ID)?.status)
    }

    @Test
    fun `marks the message as failed when its conversation is unknown`() = runTest {
        databaseRule.database.messageDao()
            .upsert(messageEntity(MESSAGE_ID, "unknown", 1_000L, status = MessageStatus.PENDING))

        val result = worker().doWork()

        assertEquals(Result.failure(), result)
        assertEquals(MessageStatus.FAILED, databaseRule.database.messageDao().findById(MESSAGE_ID)?.status)
    }

    @Test
    fun `uploads the attachments and then sends the message with them`() = runTest {
        storePending()
        val file = temporaryFolder.newFile("photo").apply { writeText("data") }
        storeAttachment(localPath = file.absolutePath)
        coEvery { attachmentSource.upload(any(), any(), any()) } returns Unit
        coEvery { attachmentSource.sendWithAttachments(any(), any()) } returns SERVER_TIME

        val result = worker().doWork()

        assertEquals(Result.success(), result)
        coVerify { attachmentSource.upload("chat/message/attachment", file, "image/png") }
        val stored = databaseRule.database.messageDao().findById(MESSAGE_ID)
        assertEquals(MessageStatus.SENT, stored?.status)
        assertEquals(Instant.parse(SERVER_TIME).toEpochMilli(), stored?.createdAtEpochMillis)
        coVerify(exactly = 0) { remoteSource.send(any()) }
    }

    @Test
    fun `retries when an attachment cannot be uploaded because the server is unreachable`() = runTest {
        storePending()
        val file = temporaryFolder.newFile("photo")
        storeAttachment(localPath = file.absolutePath)
        coEvery { attachmentSource.upload(any(), any(), any()) } throws IOException("offline")

        assertEquals(Result.retry(), worker(runAttemptCount = 0).doWork())
        coVerify(exactly = 0) { attachmentSource.sendWithAttachments(any(), any()) }
    }

    @Test
    fun `marks the message as failed when the file of an attachment is gone`() = runTest {
        storePending()
        storeAttachment(localPath = File(temporaryFolder.root, "missing").absolutePath)

        assertEquals(Result.failure(), worker().doWork())
        assertEquals(MessageStatus.FAILED, databaseRule.database.messageDao().findById(MESSAGE_ID)?.status)
    }

    @Test
    fun `fails when the message does not exist`() = runTest {
        assertEquals(Result.failure(), worker(messageId = "missing").doWork())
    }

    private suspend fun storePending() {
        databaseRule.database.messageDao()
            .upsert(messageEntity(MESSAGE_ID, CONVERSATION_ID, 1_000L, status = MessageStatus.PENDING))
    }

    private suspend fun storeAttachment(localPath: String) {
        databaseRule.database.attachmentDao().upsertAll(
            listOf(
                AttachmentEntity(
                    id = "attachment",
                    messageId = MESSAGE_ID,
                    storagePath = "chat/message/attachment",
                    mimeType = "image/png",
                    sizeBytes = 4L,
                    fileName = "photo.png",
                    localPath = localPath,
                ),
            ),
        )
    }

    private fun worker(messageId: String = MESSAGE_ID, runAttemptCount: Int = 0): SendMessageWorker {
        val database = databaseRule.database
        return TestListenableWorkerBuilder<SendMessageWorker>(context)
            .setInputData(workDataOf(SendMessageWorker.KEY_MESSAGE_ID to messageId))
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = SendMessageWorker(
                        appContext,
                        workerParameters,
                        database.messageDao(),
                        MessageDeliverer(
                            database.conversationDao(),
                            database.attachmentDao(),
                            remoteSource,
                            attachmentSource,
                        ),
                    )
                },
            )
            .build()
    }

    private companion object {
        const val MESSAGE_ID = "message"
        const val CONVERSATION_ID = "chat"
        const val SERVER_TIME = "2026-11-10T08:00:00.500Z"
        const val LAST_ATTEMPT_INDEX = 4
    }
}
