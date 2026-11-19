package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.remote.ContactRemoteSource
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.ContactSearchResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstContactRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remoteSource = mockk<ContactRemoteSource>(relaxed = true) {
        coEvery { fetchFavoriteIds(any()) } returns emptySet()
    }
    private val conversationRepository = mockk<ConversationRepository>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns CURRENT_USER_ID }
    private val repository by lazy {
        OfflineFirstContactRepository(databaseRule.database, remoteSource, conversationRepository, authRepository)
    }
    private val ana = Contact(TEST_ORG_ID, "ana", "Ana", OrgRole.STAFF)

    @Test
    fun `searching asks the server and marks the favorites`() = runTest {
        coEvery { remoteSource.searchContacts("be", CURRENT_USER_ID) } returns
            listOf(contactDto("ben"), contactDto("bea"))
        coEvery { remoteSource.fetchFavoriteIds(CURRENT_USER_ID) } returns setOf("bea")

        val result = repository.search("be") as ContactSearchResult.Found

        assertEquals(listOf("ben", "bea"), result.contacts.map { it.userId })
        assertEquals(listOf(false, true), result.contacts.map { it.isFavorite })
    }

    @Test
    fun `when the server cannot be reached the search says so`() = runTest {
        coEvery { remoteSource.searchContacts(any(), any()) } throws IOException("offline")

        assertEquals(ContactSearchResult.Unavailable, repository.search("be"))
    }

    @Test
    fun `a favorite beyond the roster is fetched too`() = runTest {
        coEvery { remoteSource.fetchContacts(CURRENT_USER_ID) } returns listOf(contactDto("ana"))
        coEvery { remoteSource.fetchFavoriteIds(CURRENT_USER_ID) } returns setOf("zed")
        coEvery { remoteSource.fetchContactsByIds(setOf("zed")) } returns listOf(contactDto("zed"))

        repository.refresh()

        repository.observeContacts().test {
            assertEquals(listOf("ana", "zed"), awaitItem().map { it.userId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favoriting someone found by searching keeps them on the roster`() = runTest {
        val zed = Contact(TEST_ORG_ID, "zed", "Zed", OrgRole.MEMBER)

        repository.setFavorite(setOf(zed), true)

        repository.observeContacts().test {
            assertEquals(listOf(true), awaitItem().map { it.isFavorite })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshing marks the people on the favorites list`() = runTest {
        coEvery { remoteSource.fetchContacts(CURRENT_USER_ID) } returns listOf(contactDto("ana"), contactDto("ben"))
        coEvery { remoteSource.fetchFavoriteIds(CURRENT_USER_ID) } returns setOf("ben")

        repository.refresh()

        repository.observeContacts().test {
            assertEquals(listOf(false, true), awaitItem().map { it.isFavorite })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favoriting is applied at once and sent to the server`() = runTest {
        databaseRule.database.contactDao().upsertAll(listOf(contactDto("ana").toEntity()))

        val result = repository.setFavorite(setOf(ana), true)

        assertEquals(PreferenceResult.Success, result)
        coVerify { remoteSource.addFavorites(TEST_ORG_ID, CURRENT_USER_ID, listOf("ana")) }
        repository.observeContacts().test {
            assertTrue(awaitItem().single().isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a refused favorite is taken back`() = runTest {
        databaseRule.database.contactDao().upsertAll(listOf(contactDto("ana").toEntity()))
        coEvery { remoteSource.addFavorites(any(), any(), any()) } throws IOException("offline")

        val result = repository.setFavorite(setOf(ana), true)

        assertEquals(PreferenceResult.Failure(PreferenceError.NETWORK_UNAVAILABLE), result)
        repository.observeContacts().test {
            assertFalse(awaitItem().single().isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removing a favorite is sent to the server`() = runTest {
        databaseRule.database.contactDao().upsertAll(listOf(contactDto("ana").toEntity(isFavorite = true)))

        repository.setFavorite(setOf(ana.copy(isFavorite = true)), false)

        coVerify { remoteSource.removeFavorites(TEST_ORG_ID, CURRENT_USER_ID, listOf("ana")) }
    }

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
