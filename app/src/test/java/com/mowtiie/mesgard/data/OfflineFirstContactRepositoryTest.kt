package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.remote.ContactRemoteSource
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.TEST_ORG_ID
import com.mowtiie.mesgard.testing.contactDto
import com.mowtiie.mesgard.testing.contactEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstContactRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remoteSource = mockk<ContactRemoteSource>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns CURRENT_USER_ID }
    private val repository by lazy {
        OfflineFirstContactRepository(databaseRule.database, remoteSource, conversationRepository, authRepository)
    }
    private val ana = Contact(TEST_ORG_ID, "ana", "Ana", OrgRole.STAFF)

    @Test
    fun `refresh stores the roster without the current user`() = runTest {
        coEvery { remoteSource.fetchContacts(CURRENT_USER_ID) } returns
            listOf(contactDto("ana", "Ana", "staff"), contactDto("ben", "Ben"))

        val result = repository.refresh()

        assertEquals(RefreshResult.Success, result)
        repository.observeContacts().test {
            assertEquals(listOf("Ana", "Ben"), awaitItem().map { it.displayName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh removes members that are no longer in the roster`() = runTest {
        databaseRule.database.contactDao().upsertAll(listOf(contactEntity("gone", name = "Gone")))
        coEvery { remoteSource.fetchContacts(any()) } returns listOf(contactDto("ana", "Ana"))

        repository.refresh()

        repository.observeContacts().test {
            assertEquals(listOf("Ana"), awaitItem().map { it.displayName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh keeps the stored roster when the server cannot be reached`() = runTest {
        databaseRule.database.contactDao().upsertAll(listOf(contactEntity("ana", name = "Ana")))
        coEvery { remoteSource.fetchContacts(any()) } throws IOException("offline")

        val result = repository.refresh()

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        repository.observeContacts().test {
            assertEquals(listOf("Ana"), awaitItem().map { it.displayName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `starting a conversation creates it and refreshes the conversation list`() = runTest {
        coEvery { remoteSource.createDirectConversation(TEST_ORG_ID, "ana") } returns "conversation-1"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Success

        val result = repository.startDirectConversation(ana)

        assertEquals(StartConversationResult.Started("conversation-1"), result)
        coVerify(exactly = 1) { conversationRepository.refresh() }
    }

    @Test
    fun `starting a conversation reports a failure to create it without refreshing`() = runTest {
        coEvery { remoteSource.createDirectConversation(any(), any()) } throws IOException("offline")

        val result = repository.startDirectConversation(ana)

        assertEquals(StartConversationResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        coVerify(exactly = 0) { conversationRepository.refresh() }
    }

    @Test
    fun `starting a conversation reports a failure to refresh the conversation list`() = runTest {
        coEvery { remoteSource.createDirectConversation(any(), any()) } returns "conversation-1"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Failure(RefreshError.UNKNOWN)

        assertEquals(StartConversationResult.Failure(RefreshError.UNKNOWN), repository.startDirectConversation(ana))
    }

    private companion object {
        const val CURRENT_USER_ID = "me"
    }
}
