package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.local.DirectPeerEntity
import com.mowtiie.mesgard.data.remote.ContactRemoteSource
import com.mowtiie.mesgard.data.remote.SEARCH_RESULT_LIMIT
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
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
class OfflineFirstContactRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remoteSource = mockk<ContactRemoteSource>(relaxed = true) {
        coEvery { fetchFavoriteIds(any()) } returns emptySet()
    }
    private val conversationRepository = mockk<ConversationRepository>()
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns CURRENT_USER_ID }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val repository by lazy {
        OfflineFirstContactRepository(
            databaseRule.database,
            remoteSource,
            conversationRepository,
            authRepository,
            scope,
        )
    }

    @After
    fun stopBackgroundWork() {
        scope.cancel()
    }

    private val ana = Contact(TEST_ORG_ID, "ana", "Ana", OrgRole.STAFF)

    @Test
    fun `searching asks the server and marks the favorites from the phone`() = runTest {
        coEvery { remoteSource.searchContacts("be", CURRENT_USER_ID) } returns
            listOf(contactDto("ben"), contactDto("bea"))
        databaseRule.database.contactDao().upsertAll(listOf(contactEntity("bea").copy(isFavorite = true)))

        val result = repository.search("be") as ContactSearchResult.Found
        coVerify(exactly = 0) { remoteSource.fetchFavoriteIds(any()) }

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
    fun `a new conversation opens as soon as it is created, and the list catches up in the background`() = runTest {
        coEvery { remoteSource.createDirectConversation(TEST_ORG_ID, "ana") } returns "conversation-1"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Success

        val result = repository.startDirectConversation(ana)

        assertEquals(StartConversationResult.Started("conversation-1"), result)
        coVerify(exactly = 1) { conversationRepository.refresh() }
    }

    @Test
    fun `a new conversation is stored with the other person before the list refreshes`() = runTest {
        coEvery { remoteSource.createDirectConversation(TEST_ORG_ID, "ana") } returns "conversation-1"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Failure(RefreshError.UNKNOWN)

        repository.startDirectConversation(ana)

        val database = databaseRule.database
        assertEquals("conversation-1", database.directPeerDao().findConversationWith("ana", TEST_ORG_ID))
        database.conversationDao().observeById("conversation-1").test {
            assertEquals("Ana", awaitItem()?.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed list refresh does not fail starting a conversation`() = runTest {
        coEvery { remoteSource.createDirectConversation(any(), any()) } returns "conversation-1"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Failure(RefreshError.UNKNOWN)

        assertEquals(StartConversationResult.Started("conversation-1"), repository.startDirectConversation(ana))
    }

    @Test
    fun `a conversation the phone already has opens without the network`() = runTest {
        val database = databaseRule.database
        database.conversationDao().upsertAll(listOf(conversationEntity("known")))
        database.directPeerDao().upsertAll(listOf(DirectPeerEntity("known", "ana")))

        val result = repository.startDirectConversation(ana)

        assertEquals(StartConversationResult.Started("known"), result)
        coVerify(exactly = 0) { remoteSource.createDirectConversation(any(), any()) }
        coVerify(exactly = 0) { conversationRepository.refresh() }
    }

    @Test
    fun `a conversation the phone has keeps its unread count when the server answers with it again`() = runTest {
        val database = databaseRule.database
        database.conversationDao().upsertAll(listOf(conversationEntity("known", unreadCount = 4)))
        coEvery { remoteSource.createDirectConversation(TEST_ORG_ID, "ana") } returns "known"
        coEvery { conversationRepository.refresh() } returns RefreshResult.Success

        repository.startDirectConversation(ana)

        database.conversationDao().observeById("known").test {
            assertEquals(4, awaitItem()?.unreadCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `starting a conversation reports a failure to create it without refreshing`() = runTest {
        coEvery { remoteSource.createDirectConversation(any(), any()) } throws IOException("offline")

        val result = repository.startDirectConversation(ana)

        assertEquals(StartConversationResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        coVerify(exactly = 0) { conversationRepository.refresh() }
    }

    @Test
    fun `the recent people are the ones of the newest conversations, and archived ones are left out`() = runTest {
        val database = databaseRule.database
        database.contactDao().upsertAll(listOf(contactEntity("ana"), contactEntity("ben"), contactEntity("cy")))
        database.conversationDao().upsertAll(
            listOf(
                conversationEntity("with-ana", createdAt = 10L),
                conversationEntity("with-ben", createdAt = 20L),
                conversationEntity("with-cy", createdAt = 30L, archivedAt = 40L),
            ),
        )
        database.directPeerDao().upsertAll(
            listOf(
                DirectPeerEntity("with-ana", "ana"),
                DirectPeerEntity("with-ben", "ben"),
                DirectPeerEntity("with-cy", "cy"),
            ),
        )
        database.messageDao().upsertAll(listOf(messageEntity("late", "with-ana", createdAt = 50L)))

        repository.observeRecentContacts(5).test {
            assertEquals(listOf("ana", "ben"), awaitItem().map { it.userId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only as many recent people as asked for are returned`() = runTest {
        val database = databaseRule.database
        database.contactDao().upsertAll(listOf(contactEntity("ana"), contactEntity("ben")))
        database.conversationDao().upsertAll(
            listOf(conversationEntity("with-ana", createdAt = 10L), conversationEntity("with-ben", createdAt = 20L)),
        )
        database.directPeerDao().upsertAll(
            listOf(DirectPeerEntity("with-ana", "ana"), DirectPeerEntity("with-ben", "ben")),
        )

        repository.observeRecentContacts(1).test {
            assertEquals(listOf("ben"), awaitItem().map { it.userId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `opening the screen twice fetches the roster once, and the favorites every time`() = runTest {
        coEvery { remoteSource.fetchContacts(CURRENT_USER_ID) } returns listOf(contactDto("ana"))

        repository.refreshIfStale()
        repository.refreshIfStale()

        coVerify(exactly = 1) { remoteSource.fetchContacts(any()) }
        coVerify(exactly = 2) { remoteSource.fetchFavoriteIds(any()) }
    }

    @Test
    fun `the roster is fetched again after a few minutes`() = runTest {
        coEvery { remoteSource.fetchContacts(CURRENT_USER_ID) } returns listOf(contactDto("ana"))
        repository.refreshIfStale()

        ShadowSystemClock.advanceBy(Duration.ofMinutes(6))
        repository.refreshIfStale()

        coVerify(exactly = 2) { remoteSource.fetchContacts(any()) }
    }

    @Test
    fun `a refresh that asks for everything always fetches the roster`() = runTest {
        coEvery { remoteSource.fetchContacts(CURRENT_USER_ID) } returns listOf(contactDto("ana"))
        repository.refreshIfStale()

        repository.refresh()

        coVerify(exactly = 2) { remoteSource.fetchContacts(any()) }
    }

    @Test
    fun `a fresh roster still gets the favorites that changed on another phone`() = runTest {
        coEvery { remoteSource.fetchContacts(CURRENT_USER_ID) } returns listOf(contactDto("ana"), contactDto("ben"))
        coEvery { remoteSource.fetchFavoriteIds(CURRENT_USER_ID) } returns setOf("ana")
        repository.refreshIfStale()
        coEvery { remoteSource.fetchFavoriteIds(CURRENT_USER_ID) } returns setOf("ben")

        repository.refreshIfStale()

        repository.observeContacts().test {
            assertEquals(mapOf("ana" to false, "ben" to true), awaitItem().associate { it.userId to it.isFavorite })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a fresh roster fetches a favorite it does not have`() = runTest {
        coEvery { remoteSource.fetchContacts(CURRENT_USER_ID) } returns listOf(contactDto("ana"))
        repository.refreshIfStale()
        coEvery { remoteSource.fetchFavoriteIds(CURRENT_USER_ID) } returns setOf("zed")
        coEvery { remoteSource.fetchContactsByIds(setOf("zed")) } returns listOf(contactDto("zed"))

        repository.refreshIfStale()

        repository.observeContacts().test {
            assertEquals(mapOf("ana" to false, "zed" to true), awaitItem().associate { it.userId to it.isFavorite })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the next user to sign in gets the roster fetched, not skipped`() = runTest {
        var currentUser = CURRENT_USER_ID
        every { authRepository.currentUserId } answers { currentUser }
        coEvery { remoteSource.fetchContacts(any()) } returns listOf(contactDto("ana"))
        repository.refreshIfStale()

        currentUser = "someone-else"
        repository.refreshIfStale()

        coVerify(exactly = 2) { remoteSource.fetchContacts(any()) }
    }

    @Test
    fun `a person who left the roster is removed and the others are kept`() = runTest {
        databaseRule.database.contactDao().upsertAll(listOf(contactEntity("ana"), contactEntity("gone")))
        coEvery { remoteSource.fetchContacts(CURRENT_USER_ID) } returns listOf(contactDto("ana"), contactDto("ben"))

        repository.refresh()

        repository.observeContacts().test {
            assertEquals(listOf("ana", "ben"), awaitItem().map { it.userId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a search that fills the server limit says there may be more`() = runTest {
        val full = (1..SEARCH_RESULT_LIMIT).map { contactDto("p$it") }
        coEvery { remoteSource.searchContacts("p", CURRENT_USER_ID) } returns full
        coEvery { remoteSource.searchContacts("p1", CURRENT_USER_ID) } returns full.take(2)

        assertTrue((repository.search("p") as ContactSearchResult.Found).mayHaveMore)
        assertFalse((repository.search("p1") as ContactSearchResult.Found).mayHaveMore)
    }

    private companion object {
        const val CURRENT_USER_ID = "me"
    }
}
