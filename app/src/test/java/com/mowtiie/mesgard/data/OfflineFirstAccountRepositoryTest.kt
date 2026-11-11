package com.mowtiie.mesgard.data

import app.cash.turbine.test
import com.mowtiie.mesgard.data.local.AccountEntity
import com.mowtiie.mesgard.data.local.AccountMembershipEntity
import com.mowtiie.mesgard.data.remote.AccountMembershipDto
import com.mowtiie.mesgard.data.remote.AccountRemoteSource
import com.mowtiie.mesgard.data.remote.OrganizationNameDto
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.testing.InMemoryDatabaseRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstAccountRepositoryTest {
    @get:Rule
    val databaseRule = InMemoryDatabaseRule()

    private val remoteSource = mockk<AccountRemoteSource>()
    private val authRepository = mockk<AuthRepository> {
        every { currentUserId } returns "me"
        every { currentUserEmail } returns "ana@school.test"
    }
    private val repository by lazy {
        OfflineFirstAccountRepository(databaseRule.database, remoteSource, authRepository)
    }

    @Test
    fun `refresh stores the profile, email and memberships`() = runTest {
        coEvery { remoteSource.fetchDisplayName("me") } returns "Ana Reyes"
        coEvery { remoteSource.fetchMemberships("me") } returns
            listOf(AccountMembershipDto("o1", "staff", OrganizationNameDto("Rizal National High School")))

        val result = repository.refresh()

        assertEquals(RefreshResult.Success, result)
        repository.observeAccount().test {
            val account = awaitItem()!!
            assertEquals("Ana Reyes", account.displayName)
            assertEquals("ana@school.test", account.email)
            assertEquals(
                listOf(AccountMembership("o1", "Rizal National High School", OrgRole.STAFF)),
                account.memberships,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh replaces memberships the user no longer has`() = runTest {
        databaseRule.database.accountDao().upsertAccount(AccountEntity("me", "Ana", null))
        databaseRule.database.accountDao().upsertMemberships(
            listOf(AccountMembershipEntity("old", "Old school", "student")),
        )
        coEvery { remoteSource.fetchDisplayName(any()) } returns "Ana"
        coEvery { remoteSource.fetchMemberships(any()) } returns
            listOf(AccountMembershipDto("o1", "org_admin", OrganizationNameDto("New school")))

        repository.refresh()

        repository.observeAccount().test {
            assertEquals(listOf("New school"), awaitItem()!!.memberships.map { it.organizationName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `there is no account until it has been loaded`() = runTest {
        repository.observeAccount().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh keeps the stored account when the server cannot be reached`() = runTest {
        databaseRule.database.accountDao().upsertAccount(AccountEntity("me", "Ana", null))
        coEvery { remoteSource.fetchDisplayName(any()) } throws IOException("offline")

        val result = repository.refresh()

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        repository.observeAccount().test {
            assertEquals("Ana", awaitItem()!!.displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh fails when the profile does not exist`() = runTest {
        coEvery { remoteSource.fetchDisplayName(any()) } returns null
        coEvery { remoteSource.fetchMemberships(any()) } returns emptyList()

        assertEquals(RefreshResult.Failure(RefreshError.UNKNOWN), repository.refresh())
    }

    @Test
    fun `refresh fails when nobody is signed in`() = runTest {
        every { authRepository.currentUserId } returns null

        assertEquals(RefreshResult.Failure(RefreshError.UNKNOWN), repository.refresh())
    }
}
