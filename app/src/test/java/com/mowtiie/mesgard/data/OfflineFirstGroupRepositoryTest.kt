package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.remote.GroupRemoteSource
import com.mowtiie.mesgard.data.remote.MembershipDto
import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.TEST_ORG_ID
import com.mowtiie.mesgard.testing.groupDto
import com.mowtiie.mesgard.testing.groupEntity
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
class OfflineFirstGroupRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remoteSource = mockk<GroupRemoteSource>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns CURRENT_USER_ID }
    private val repository by lazy {
        OfflineFirstGroupRepository(databaseRule.database, remoteSource, conversationRepository, authRepository)
    }
    private val sectionA = Group("section-a", TEST_ORG_ID, "Section A", canStartConversation = true)

    @Test
    fun `refresh stores the groups`() = runTest {
        coEvery { remoteSource.fetchMemberships(CURRENT_USER_ID) } returns listOf(MembershipDto(TEST_ORG_ID, "staff"))
        coEvery { remoteSource.fetchGroups() } returns listOf(groupDto("b", "Band"), groupDto("a", "Art"))

        val result = repository.refresh()

        assertEquals(RefreshResult.Success, result)
        repository.observeGroups().test {
            assertEquals(listOf("Art", "Band"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `staff and admins can start group conversations but students cannot`() = runTest {
        coEvery { remoteSource.fetchGroups() } returns listOf(groupDto("a"))

        coEvery { remoteSource.fetchMemberships(CURRENT_USER_ID) } returns listOf(MembershipDto(TEST_ORG_ID, "student"))
        repository.refresh()
        repository.observeGroups().test {
            assertEquals(listOf(false), awaitItem().map { it.canStartConversation })
            cancelAndIgnoreRemainingEvents()
        }

        coEvery { remoteSource.fetchMemberships(CURRENT_USER_ID) } returns
            listOf(MembershipDto(TEST_ORG_ID, "org_admin"))
        repository.refresh()
        repository.observeGroups().test {
            assertEquals(listOf(true), awaitItem().map { it.canStartConversation })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a role in another organization does not allow starting a group conversation`() = runTest {
        coEvery { remoteSource.fetchMemberships(CURRENT_USER_ID) } returns listOf(MembershipDto("other-org", "staff"))
        coEvery { remoteSource.fetchGroups() } returns listOf(groupDto("a"))

        repository.refresh()

        repository.observeGroups().test {
            assertEquals(listOf(false), awaitItem().map { it.canStartConversation })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh removes groups that are no longer visible`() = runTest {
        databaseRule.database.groupDao().upsertAll(listOf(groupEntity("gone")))
        coEvery { remoteSource.fetchMemberships(any()) } returns emptyList()
        coEvery { remoteSource.fetchGroups() } returns listOf(groupDto("a", "Art"))

        repository.refresh()

        repository.observeGroups().test {
            assertEquals(listOf("Art"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh keeps the stored groups when the server cannot be reached`() = runTest {
        databaseRule.database.groupDao().upsertAll(listOf(groupEntity("a", name = "Art")))
        coEvery { remoteSource.fetchMemberships(any()) } throws IOException("offline")

        val result = repository.refresh()

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        repository.observeGroups().test {
            assertEquals(listOf("Art"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh fails when nobody is signed in`() = runTest {
        every { authRepository.currentUserId } returns null

        assertEquals(RefreshResult.Failure(RefreshError.UNKNOWN), repository.refresh())
    }

    @Test
    fun `starting a conversation creates it and refreshes the conversation list`() = runTest {
        coEvery { remoteSource.createGroupConversation("section-a") } returns "conversation-1"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Success

        val result = repository.startGroupConversation(sectionA)

        assertEquals(StartConversationResult.Started("conversation-1"), result)
        coVerify(exactly = 1) { conversationRepository.refresh() }
    }

    @Test
    fun `starting a conversation reports a failure to create it without refreshing`() = runTest {
        coEvery { remoteSource.createGroupConversation(any()) } throws IOException("offline")

        val result = repository.startGroupConversation(sectionA)

        assertEquals(StartConversationResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        coVerify(exactly = 0) { conversationRepository.refresh() }
    }

    @Test
    fun `starting a conversation reports a failure to refresh the conversation list`() = runTest {
        coEvery { remoteSource.createGroupConversation(any()) } returns "conversation-1"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Failure(RefreshError.UNKNOWN)

        assertEquals(StartConversationResult.Failure(RefreshError.UNKNOWN), repository.startGroupConversation(sectionA))
    }

    private companion object {
        const val CURRENT_USER_ID = "me"
    }
}
