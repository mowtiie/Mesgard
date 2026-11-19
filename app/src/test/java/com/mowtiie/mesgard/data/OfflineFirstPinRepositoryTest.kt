package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.local.PinnedMessageEntity
import com.mowtiie.mesgard.data.local.ProfileEntity
import com.mowtiie.mesgard.data.local.upsert
import com.mowtiie.mesgard.data.remote.PinRemoteSource
import com.mowtiie.mesgard.data.remote.PinnedMessageDto
import com.mowtiie.mesgard.data.remote.PinnedRowDto
import com.mowtiie.mesgard.data.remote.SenderNameDto
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.PinnedMessage
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.TEST_ORG_ID
import com.mowtiie.mesgard.testing.TEST_SENDER_ID
import com.mowtiie.mesgard.testing.conversationEntity
import com.mowtiie.mesgard.testing.messageEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstPinRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remote = mockk<PinRemoteSource>()
    private val account = MutableStateFlow<Account?>(null)
    private val accountRepository = mockk<AccountRepository> { every { observeAccount() } returns account }
    private val clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    private val repository by lazy {
        OfflineFirstPinRepository(remote, databaseRule.database, accountRepository, clock)
    }
    private val pinDao by lazy { databaseRule.database.pinnedMessageDao() }

    @Before
    fun storeConversation() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("chat")))
    }

    private fun roleIs(role: OrgRole) {
        account.value = Account("Me", null, listOf(AccountMembership(TEST_ORG_ID, "Example", role)))
    }

    private fun row(id: String, pinnedAt: String, body: String = "Pinned $id") =
        PinnedRowDto(id, pinnedAt, PinnedMessageDto(body, SenderNameDto("Ana")))

    private fun stored(id: String, pinnedAt: Long) = PinnedMessageEntity(id, "chat", pinnedAt, "Ana", "Pinned $id")

    @Test
    fun `refresh replaces the stored pins with the server pins, the latest pinned first`() = runTest {
        pinDao.upsertAll(listOf(stored("old", 1L)))
        coEvery { remote.fetch("chat") } returns listOf(
            row("a", "2026-11-18T08:00:00+00:00"),
            row("b", "2026-11-19T08:00:00+00:00", body = "Due on Friday"),
        )

        assertEquals(RefreshResult.Success, repository.refresh("chat"))

        assertEquals(
            listOf(PinnedMessage("b", "Ana", "Due on Friday"), PinnedMessage("a", "Ana", "Pinned a")),
            repository.observePins("chat").first(),
        )
    }

    @Test
    fun `a refresh that fails keeps the stored pins`() = runTest {
        pinDao.upsertAll(listOf(stored("kept", 1L)))
        coEvery { remote.fetch(any()) } throws IOException("offline")

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.refresh("chat"))
        assertEquals(listOf("kept"), repository.observePins("chat").first().map { it.messageId })
    }

    @Test
    fun `pins of another conversation are not shown`() = runTest {
        pinDao.upsertAll(listOf(stored("here", 1L), stored("there", 2L).copy(conversationId = "other")))

        assertEquals(listOf("here"), repository.observePins("chat").first().map { it.messageId })
    }

    @Test
    fun `only administrators and staff of the organization of the conversation can pin`() = runTest {
        roleIs(OrgRole.STAFF)
        assertTrue(repository.observeCanPin("chat").first())
        roleIs(OrgRole.ORG_ADMIN)
        assertTrue(repository.observeCanPin("chat").first())
        roleIs(OrgRole.MEMBER)
        assertFalse(repository.observeCanPin("chat").first())
        account.value = Account("Me", null, listOf(AccountMembership("another-org", "Other", OrgRole.ORG_ADMIN)))
        assertFalse(repository.observeCanPin("chat").first())
    }

    @Test
    fun `pinning shows the pin before the server answers and sends the organization of the conversation`() = runTest {
        databaseRule.database.profileDao().upsertAll(listOf(ProfileEntity(TEST_SENDER_ID, "Ana")))
        databaseRule.database.messageDao().upsert(messageEntity("m1", "chat", 1_000L, body = "Due on Friday"))
        var shownWhileWaiting: List<PinnedMessage> = emptyList()
        coEvery { remote.pin("chat", TEST_ORG_ID, "m1") } coAnswers {
            shownWhileWaiting = repository.observePins("chat").first()
        }

        assertEquals(RefreshResult.Success, repository.pin("chat", "m1"))

        assertEquals(listOf(PinnedMessage("m1", "Ana", "Due on Friday")), shownWhileWaiting)
        coVerify { remote.pin("chat", TEST_ORG_ID, "m1") }
    }

    @Test
    fun `a pin the server refuses is taken back`() = runTest {
        databaseRule.database.messageDao().upsert(messageEntity("m1", "chat", 1_000L))
        coEvery { remote.pin(any(), any(), any()) } throws IOException("offline")

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.pin("chat", "m1"))

        assertNull(pinDao.find("m1"))
    }

    @Test
    fun `pinning in a conversation that is not stored is refused`() = runTest {
        assertEquals(RefreshResult.Failure(RefreshError.UNKNOWN), repository.pin("unknown", "m1"))
    }

    @Test
    fun `unpinning removes the pin at once and a refusal puts it back`() = runTest {
        pinDao.upsertAll(listOf(stored("m1", 1L), stored("m2", 2L)))
        coEvery { remote.unpin("m1") } returns Unit
        coEvery { remote.unpin("m2") } throws IOException("offline")

        assertEquals(RefreshResult.Success, repository.unpin("m1"))
        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.unpin("m2"))

        repository.observePins("chat").test {
            assertEquals(listOf("m2"), awaitItem().map { it.messageId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
