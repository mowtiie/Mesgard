package com.mowtiie.mesgard.data.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.mowtiie.mesgard.data.local.upsert
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageEntity
import io.github.jan.supabase.exceptions.RestException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarkReadWorkerTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val remoteSource = mockk<MessageRemoteSource>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns CURRENT_USER_ID }

    @Before
    fun storeConversation() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity(CONVERSATION_ID)))
    }

    @Test
    fun `marks the conversation as read up to the newest incoming message`() = runTest {
        val dao = databaseRule.database.messageDao()
        dao.upsert(messageEntity("old", CONVERSATION_ID, 1_000L))
        dao.upsert(messageEntity("new", CONVERSATION_ID, 2_000L))
        dao.upsert(messageEntity("mine", CONVERSATION_ID, 3_000L).copy(senderId = CURRENT_USER_ID))
        coEvery { remoteSource.markConversationRead(any(), any()) } returns Unit

        val result = worker().doWork()

        assertEquals(Result.success(), result)
        coVerify { remoteSource.markConversationRead(CONVERSATION_ID, "new") }
    }

    @Test
    fun `does nothing when there is no incoming message`() = runTest {
        databaseRule.database.messageDao()
            .upsert(messageEntity("mine", CONVERSATION_ID, 1_000L).copy(senderId = CURRENT_USER_ID))

        val result = worker().doWork()

        assertEquals(Result.success(), result)
        coVerify(exactly = 0) { remoteSource.markConversationRead(any(), any()) }
    }

    @Test
    fun `retries when the network fails and gives up on the last attempt`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity("new", CONVERSATION_ID, 2_000L))
        coEvery { remoteSource.markConversationRead(any(), any()) } throws IOException("offline")

        assertEquals(Result.retry(), worker(runAttemptCount = 0).doWork())
        assertEquals(Result.failure(), worker(runAttemptCount = LAST_ATTEMPT_INDEX).doWork())
    }

    @Test
    fun `fails without retrying when the server rejects the marker`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity("new", CONVERSATION_ID, 2_000L))
        coEvery { remoteSource.markConversationRead(any(), any()) } throws mockk<RestException>(relaxed = true)

        assertEquals(Result.failure(), worker().doWork())
    }

    @Test
    fun `fails when nobody is signed in`() = runTest {
        every { authRepository.currentUserId } returns null

        assertEquals(Result.failure(), worker().doWork())
    }

    private fun worker(runAttemptCount: Int = 0): MarkReadWorker = TestListenableWorkerBuilder<MarkReadWorker>(context)
        .setInputData(workDataOf(MarkReadWorker.KEY_CONVERSATION_ID to CONVERSATION_ID))
        .setRunAttemptCount(runAttemptCount)
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = MarkReadWorker(
                    appContext,
                    workerParameters,
                    databaseRule.database.messageDao(),
                    remoteSource,
                    authRepository,
                )
            },
        )
        .build()

    private companion object {
        const val CONVERSATION_ID = "chat"
        const val CURRENT_USER_ID = "me"
        const val LAST_ATTEMPT_INDEX = 4
    }
}
