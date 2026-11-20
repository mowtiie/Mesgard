package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.remote.CONVERSATION_PAGE_SIZE
import com.mowtiie.mesgard.data.remote.ConversationRemoteSource
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MessageRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.TEST_ORG_ID
import com.mowtiie.mesgard.testing.contactEntity
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.summaryDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val ASKED_TOGETHER_TIMEOUT_MS = 1_000L

@RunWith(RobolectricTestRunner::class)
class OfflineFirstConversationRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remoteSource = mockk<ConversationRemoteSource>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "me" }

    init {
        coEvery { remoteSource.fetchMutedConversationIds(any()) } returns emptySet()
        coEvery { remoteSource.fetchDirectPeerIds(any(), any()) } returns emptyMap()
    }

    private val messageRepository = mockk<MessageRepository> {
        coEvery { refresh(any()) } returns RefreshResult.Success
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val repository by lazy {
        OfflineFirstConversationRepository(
            databaseRule.database,
            remoteSource,
            authRepository,
            messageRepository,
            scope,
        )
    }

    @After
    fun stopSharing() {
        scope.cancel()
    }

    @Test
    fun `the summaries and the muted conversations are asked at the same time`() = runTest {
        val mutedAsked = CompletableDeferred<Unit>()
        coEvery { remoteSource.fetchSummaries() } coAnswers {
            withTimeout(ASKED_TOGETHER_TIMEOUT_MS) { mutedAsked.await() }
            listOf(summaryDto("a"))
        }
        coEvery { remoteSource.fetchMutedConversationIds("me") } coAnswers {
            mutedAsked.complete(Unit)
            setOf("a")
        }

        assertEquals(RefreshResult.Success, repository.refresh())

        repository.observeConversation("a").test {
            assertEquals(true, awaitItem()?.isMuted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed muted lookup fails the refresh and stores nothing`() = runTest {
        coEvery { remoteSource.fetchSummaries() } returns listOf(summaryDto("a"))
        coEvery { remoteSource.fetchMutedConversationIds("me") } throws IOException("offline")

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.refresh())

        repository.observeConversation("a").test {
            assertEquals(null, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the list is read once and every screen gets the same list`() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("a")))

        repository.observeConversations().test {
            val first = awaitItem()
            repository.observeConversations().test {
                assertSame(first, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a change reaches every screen that watches the list`() = runTest {
        val dao = databaseRule.database.conversationDao()
        dao.upsertAll(listOf(conversationEntity("a")))

        repository.observeConversations().test {
            assertEquals(listOf("a"), awaitItem().map { it.id })
            repository.observeConversations().test {
                assertEquals(listOf("a"), awaitItem().map { it.id })
                dao.upsertAll(listOf(conversationEntity("b")))
                assertEquals(setOf("a", "b"), awaitItem().map { it.id }.toSet())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(setOf("a", "b"), awaitItem().map { it.id }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `after a reconnection the ten newest conversations with unread messages get their latest messages`() = runTest {
        val summaries = (1..12).map { day ->
            summaryDto("c$day", lastMessageCreatedAt = "2026-11-%02dT08:00:00+00:00".format(day))
        }
        coEvery { remoteSource.fetchSummaries() } returns summaries

        repository.refreshAfterReconnect()

        coVerifyOrder {
            messageRepository.refresh("c12")
            messageRepository.refresh("c11")
        }
        coVerify(exactly = 10) { messageRepository.refresh(any()) }
        coVerify(exactly = 0) { messageRepository.refresh("c1") }
        coVerify(exactly = 0) { messageRepository.refresh("c2") }
    }

    @Test
    fun `no conversation is caught up when the list could not be refreshed`() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("unread", unreadCount = 2)))
        coEvery { remoteSource.fetchSummaries() } throws IOException("offline")

        val result = repository.refreshAfterReconnect()

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        coVerify(exactly = 0) { messageRepository.refresh(any()) }
    }

    @Test
    fun `refresh marks the conversations the user muted`() = runTest {
        coEvery { remoteSource.fetchSummaries() } returns listOf(summaryDto("quiet"), summaryDto("loud"))
        coEvery { remoteSource.fetchMutedConversationIds("me") } returns setOf("quiet")

        repository.refresh()

        repository.observeConversations().test {
            assertEquals(mapOf("quiet" to true, "loud" to false), awaitItem().associate { it.id to it.isMuted })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a direct conversation with a favorite person is marked, and one with anyone else is not`() = runTest {
        databaseRule.database.contactDao().upsertAll(
            listOf(contactEntity("ana").copy(isFavorite = true), contactEntity("ben")),
        )
        coEvery { remoteSource.fetchSummaries() } returns listOf(summaryDto("with-ana"), summaryDto("with-ben"))
        coEvery { remoteSource.fetchDirectPeerIds(any(), "me") } returns
            mapOf("with-ana" to "ana", "with-ben" to "ben")

        repository.refresh()

        repository.observeConversations().test {
            val flags = awaitItem().associate { it.id to it.isWithFavorite }
            assertEquals(mapOf("with-ana" to true, "with-ben" to false), flags)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `making a person a favorite marks the conversation without another refresh`() = runTest {
        val contacts = databaseRule.database.contactDao()
        contacts.upsertAll(listOf(contactEntity("ana")))
        coEvery { remoteSource.fetchSummaries() } returns listOf(summaryDto("with-ana"))
        coEvery { remoteSource.fetchDirectPeerIds(any(), "me") } returns mapOf("with-ana" to "ana")
        repository.refresh()

        repository.observeConversations().test {
            assertEquals(false, awaitItem().single().isWithFavorite)
            contacts.updateFavorite(TEST_ORG_ID, listOf("ana"), isFavorite = true)
            assertEquals(true, awaitItem().single().isWithFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the other person of a direct conversation is asked for once, and groups are never asked`() = runTest {
        coEvery { remoteSource.fetchSummaries() } returns listOf(summaryDto("direct"), summaryDto("group", "group"))
        coEvery { remoteSource.fetchDirectPeerIds(any(), "me") } returns mapOf("direct" to "ana")

        repository.refresh()
        repository.refresh()

        coVerify(exactly = 1) { remoteSource.fetchDirectPeerIds(listOf("direct"), "me") }
    }

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
