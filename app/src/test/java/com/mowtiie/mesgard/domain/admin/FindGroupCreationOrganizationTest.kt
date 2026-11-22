package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupCreationPolicy
import com.mowtiie.mesgard.domain.model.GroupCreationRules
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.GroupCreationRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FindGroupCreationOrganizationTest {
    private val groupCreation = mockk<GroupCreationRepository>()

    private fun find(vararg memberships: AccountMembership): FindGroupCreationOrganization {
        val accounts = mockk<AccountRepository> {
            every { observeAccount() } returns flowOf(Account("Ana", null, memberships.toList()))
            coEvery { refresh() } returns RefreshResult.Success
        }
        return FindGroupCreationOrganization(accounts, groupCreation)
    }

    private fun rules(canCreate: Boolean) = AdminResult.Success(
        GroupCreationRules(GroupCreationPolicy.EVERYONE, canCreate, created = 0, limit = 5),
    )

    @Test
    fun `prefers an organization the person administers without asking the server`() = runTest {
        val admin = AccountMembership("a", "Admin School", OrgRole.ORG_ADMIN)

        assertEquals(admin, find(AccountMembership("b", "Other", OrgRole.MEMBER), admin)())
    }

    @Test
    fun `otherwise takes the first organization whose policy lets the person create groups`() = runTest {
        val closed = AccountMembership("b", "Closed", OrgRole.MEMBER)
        val open = AccountMembership("c", "Open", OrgRole.MEMBER)
        coEvery { groupCreation.rules("b") } returns rules(canCreate = false)
        coEvery { groupCreation.rules("c") } returns rules(canCreate = true)

        assertEquals(open, find(closed, open)())
    }

    @Test
    fun `waits for the account when it is not stored yet`() = runTest {
        val admin = AccountMembership("a", "Admin School", OrgRole.ORG_ADMIN)
        val accounts = mockk<AccountRepository> {
            every { observeAccount() } returns flow {
                emit(null)
                emit(Account("Ana", null, emptyList()))
                emit(Account("Ana", null, listOf(admin)))
            }
            coEvery { refresh() } returns RefreshResult.Success
        }

        assertEquals(admin, FindGroupCreationOrganization(accounts, groupCreation)())
    }

    @Test
    fun `finds nothing when no policy allows it or the server cannot be asked`() = runTest {
        coEvery { groupCreation.rules("b") } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)

        assertNull(find(AccountMembership("b", "Closed", OrgRole.MEMBER))())
    }
}
