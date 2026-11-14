package com.mowtiie.mesgard.ui.admin.groups

import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GroupsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns MutableStateFlow(
            Account("Admin", null, listOf(AccountMembership(ORG, "Rizal National", OrgRole.ORG_ADMIN))),
        )
    }
    private val groupAdminRepository = mockk<GroupAdminRepository>()
    private val groups = listOf(AdminGroup("g1", "Grade 10 A", 28), AdminGroup("g2", "Science Department", 5))

    private fun viewModel() = GroupsViewModel(ObserveAdminOrganization(accountRepository), groupAdminRepository)

    @Test
    fun `loads the groups of the administered organization`() {
        coEvery { groupAdminRepository.listGroups(ORG) } returns AdminResult.Success(groups)

        val state = viewModel().uiState.value

        assertEquals(groups, state.groups)
        assertFalse(state.isLoading)
    }

    @Test
    fun `searches the groups by name and says when nothing matches`() {
        coEvery { groupAdminRepository.listGroups(ORG) } returns AdminResult.Success(groups)
        val viewModel = viewModel()

        viewModel.onQueryChanged("science")
        assertEquals(listOf("Science Department"), viewModel.uiState.value.visibleGroups.map { it.name })

        viewModel.onQueryChanged("zzz")
        assertTrue(viewModel.uiState.value.showNoResults)
    }

    @Test
    fun `shows the empty message when the organization has no groups`() {
        coEvery { groupAdminRepository.listGroups(ORG) } returns AdminResult.Success(emptyList())

        assertTrue(viewModel().uiState.value.showEmptyMessage)
    }

    @Test
    fun `reports a failed load`() {
        coEvery { groupAdminRepository.listGroups(ORG) } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        val viewModel = viewModel()

        assertEquals(AdminError.NETWORK_UNAVAILABLE, viewModel.uiState.value.error)
        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `creates a group and loads the list again`() {
        coEvery { groupAdminRepository.listGroups(ORG) } returns AdminResult.Success(groups)
        coEvery { groupAdminRepository.createGroup(ORG, "Chess Club") } returns AdminResult.Success("g3")
        val viewModel = viewModel()

        viewModel.onCreateConfirmed("Chess Club")

        coVerify(exactly = 1) { groupAdminRepository.createGroup(ORG, "Chess Club") }
        coVerify(exactly = 2) { groupAdminRepository.listGroups(ORG) }
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `does not create a group without a name or when the server refuses`() {
        coEvery { groupAdminRepository.listGroups(ORG) } returns AdminResult.Success(groups)
        coEvery { groupAdminRepository.createGroup(ORG, "Chess Club") } returns
            AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onCreateConfirmed("   ")
        coVerify(exactly = 0) { groupAdminRepository.createGroup(any(), any()) }

        viewModel.onCreateConfirmed("Chess Club")
        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        coVerify(exactly = 1) { groupAdminRepository.listGroups(ORG) }
    }

    private companion object {
        const val ORG = "org"
    }
}
