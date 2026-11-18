package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.PinRemoteSource
import com.mowtiie.mesgard.data.remote.PinnedMessageDto
import com.mowtiie.mesgard.data.remote.PinnedRowDto
import com.mowtiie.mesgard.data.remote.SenderNameDto
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.PinsOutcome
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import com.mowtiie.mesgard.testing.TEST_ORG_ID
import com.mowtiie.mesgard.testing.conversationEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
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
class RemotePinRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remote = mockk<PinRemoteSource>()
    private val account = MutableStateFlow<Account?>(null)
    private val accountRepository = mockk<AccountRepository> { every { observeAccount() } returns account }
    private val repository by lazy { RemotePinRepository(remote, databaseRule.database, accountRepository) }

    @Before
    fun storeConversation() = runTest {
        databaseRule.database.conversationDao().upsertAll(listOf(conversationEntity("chat")))
    }

    private fun roleIs(role: OrgRole) {
        account.value = Account("Me", null, listOf(AccountMembership(TEST_ORG_ID, "Example", role)))
    }

    @Test
    fun `reads the pinned messages with who wrote them`() = runTest {
        coEvery { remote.fetch("chat") } returns
            listOf(PinnedRowDto("m1", PinnedMessageDto("Due on Friday", SenderNameDto("Ana"))))

        val pins = (repository.fetch("chat") as PinsOutcome.Loaded).pins

        assertEquals("Ana", pins.single().senderName)
        assertEquals("Due on Friday", pins.single().body)
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
    fun `pinning sends the organization of the conversation`() = runTest {
        coEvery { remote.pin("chat", TEST_ORG_ID, "m1") } returns Unit

        assertEquals(RefreshResult.Success, repository.pin("chat", "m1"))
        coVerify { remote.pin("chat", TEST_ORG_ID, "m1") }
    }

    @Test
    fun `pinning in a conversation that is not stored is refused`() = runTest {
        assertEquals(RefreshResult.Failure(RefreshError.UNKNOWN), repository.pin("unknown", "m1"))
    }

    @Test
    fun `a connection problem is reported`() = runTest {
        coEvery { remote.unpin("m1") } throws IOException("offline")
        coEvery { remote.fetch(any()) } throws IOException("offline")

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.unpin("m1"))
        assertEquals(PinsOutcome.Failure(RefreshError.NETWORK_UNAVAILABLE), repository.fetch("chat"))
    }
}
