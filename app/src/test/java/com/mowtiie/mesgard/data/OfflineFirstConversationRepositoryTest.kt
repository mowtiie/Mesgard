package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.remote.CONVERSATION_PAGE_SIZE
import com.mowtiie.mesgard.data.remote.ConversationRemoteSource
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.summaryDto
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstConversationRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remoteSource = mockk<ConversationRemoteSource>()
    private val repository by lazy { OfflineFirstConversationRepository(databaseRule.database, remoteSource) }

    @Test
    fun `refresh stores the conversations with their last message`() = runTest {
        coEvery { remoteSource.fetchSummaries() } returns listOf(summaryDto("chat"))

        val result = repository.refresh()

        assertEquals(RefreshResult.Success, result)
        repository.observeConversations().test {
            val conversation = awaitItem().single()
            assertEquals("chat", conversation.id)
            assertEquals("Last message of chat", conversation.lastMessagePreview)
            assertEquals(2, conversation.unreadCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh removes conversations the user no longer belongs to`() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("left")))
        coEvery { remoteSource.fetchSummaries() } returns listOf(summaryDto("current"))

        repository.refresh()

        repository.observeConversations().test {
            assertEquals(listOf("current"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh keeps older conversations when the server returned a full page`() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("older")))
        coEvery { remoteSource.fetchSummaries() } returns List(CONVERSATION_PAGE_SIZE) { summaryDto("page-$it") }

        repository.refresh()

        databaseRule.database.conversationDao().observeList(CONVERSATION_PAGE_SIZE + 1).test {
            assertEquals(true, awaitItem().any { it.id == "older" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reports a network failure and keeps the stored conversations`() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("stored")))
        coEvery { remoteSource.fetchSummaries() } throws IOException("offline")

        val result = repository.refresh()

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        repository.observeConversations().test {
            assertEquals(listOf("stored"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reports an unknown failure for an unsupported conversation type`() = runTest {
        coEvery { remoteSource.fetchSummaries() } returns listOf(summaryDto("chat", type = "channel"))

        val result = repository.refresh()

        assertEquals(RefreshResult.Failure(RefreshError.UNKNOWN), result)
    }
}
