package com.mowtiie.mesgard.ui.admin

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.AuditEntry
import com.mowtiie.mesgard.domain.model.AuditPage
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AuditLogRepository
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.domain.repository.ROSTER_LIMIT
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdministrationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val adminAccount = Account("Admin Cruz", null, listOf(AccountMembership(ORG, "Rizal", OrgRole.ORG_ADMIN)))
    private val account = MutableStateFlow<Account?>(adminAccount)
    private val contacts = MutableStateFlow(listOf(Contact(ORG, "u1", "Ana Reyes", OrgRole.STUDENT)))
    private val accountRepository = mockk<AccountRepository> { every { observeAccount() } returns account }
    private val contactRepository = mockk<ContactRepository> {
        every { observeContacts() } returns contacts
        coEvery { refresh() } returns RefreshResult.Success
    }
    private val groupAdminRepository = mockk<GroupAdminRepository> {
        coEvery { listGroups(ORG) } returns AdminResult.Success(listOf(AdminGroup("g1", "Grade 7", 30)))
    }
    private val auditLogRepository = mockk<AuditLogRepository> {
        coEvery { fetchPage(ORG, null) } returns AdminResult.Success(AuditPage(entries(5), next = null))
    }
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "me" }

    private fun entries(count: Int) =
        (1..count).map { AuditEntry("id-$it", "me", "member.added", "u1", emptyMap(), it.toLong()) }

    private fun viewModel() = AdministrationViewModel(
        ObserveAdminOrganization(accountRepository),
        contactRepository,
        groupAdminRepository,
        auditLogRepository,
        accountRepository,
        authRepository,
    )

    @Test
    fun `shows the organization and the counts once loaded`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.isAdmin)
            assertEquals("Rizal", state.organizationName)
            assertEquals(1, state.memberCount)
            assertEquals(1, state.groupCount)
            assertFalse(state.isRefreshing)
        }
    }

    @Test
    fun `keeps only the latest changes for the overview`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(RECENT_ACTIVITY_COUNT, state.recentActivity.size)
            assertEquals(listOf("id-1", "id-2", "id-3"), state.recentActivity.map { it.id })
            assertFalse(state.showNoActivity)
        }
    }

    @Test
    fun `says there is no activity when the log is empty`() = runTest {
        coEvery { auditLogRepository.fetchPage(ORG, null) } returns AdminResult.Success(AuditPage(emptyList(), null))

        viewModel().uiState.test { assertTrue(expectMostRecentItem().showNoActivity) }
    }

    @Test
    fun `counts only the people of the administered organization`() = runTest {
        contacts.value = listOf(
            Contact(ORG, "u1", "Ana Reyes", OrgRole.STUDENT),
            Contact(ORG, "u2", "Ben Cruz", OrgRole.STAFF),
            Contact("other", "u3", "Cy Lim", OrgRole.STUDENT),
        )

        viewModel().uiState.test { assertEquals(2, expectMostRecentItem().memberCount) }
    }

    @Test
    fun `marks a member count that reaches the roster limit as a minimum`() = runTest {
        contacts.value = (1..ROSTER_LIMIT).map { Contact(ORG, "u$it", "Person $it", OrgRole.STUDENT) }

        viewModel().uiState.test { assertTrue(expectMostRecentItem().isMemberCountAtLeast) }
    }

    @Test
    fun `reports the failure and shows no counts it could not load`() = runTest {
        coEvery { groupAdminRepository.listGroups(ORG) } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        coEvery { auditLogRepository.fetchPage(ORG, null) } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)

        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(AdminError.NETWORK_UNAVAILABLE, state.error)
            assertNull(state.groupCount)
            assertFalse(state.hasLoadedActivity)
            assertFalse(state.isRefreshing)
        }
    }

    @Test
    fun `keeps the earlier counts when a refresh fails`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            expectMostRecentItem()
            coEvery { groupAdminRepository.listGroups(ORG) } returns
                AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)

            viewModel.onRefresh()

            val state = expectMostRecentItem()
            assertEquals(1, state.groupCount)
            assertEquals(AdminError.NETWORK_UNAVAILABLE, state.error)
            viewModel.onErrorShown()
            assertNull(expectMostRecentItem().error)
        }
    }

    @Test
    fun `is not an admin view for someone who administers nothing`() = runTest {
        account.value = Account("Ana Reyes", null, listOf(AccountMembership(ORG, "Rizal", OrgRole.STAFF)))

        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isAdmin)
            assertEquals(AdminError.NOT_ALLOWED, state.error)
        }
    }

    private companion object {
        const val ORG = "org-1"
    }
}
