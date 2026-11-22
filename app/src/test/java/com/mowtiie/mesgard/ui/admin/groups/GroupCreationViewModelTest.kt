package com.mowtiie.mesgard.ui.admin.groups

import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupCreationPolicy
import com.mowtiie.mesgard.domain.model.GroupCreationRules
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.GroupCreationRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GroupCreationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns MutableStateFlow(
            Account("Admin", null, listOf(AccountMembership(ORG, "Rizal National", OrgRole.ORG_ADMIN))),
        )
    }
    private val repository = mockk<GroupCreationRepository> {
        coEvery { rules(ORG) } returns AdminResult.Success(
            GroupCreationRules(GroupCreationPolicy.STAFF, canCreate = true, created = 0, limit = 5),
        )
    }

    private fun viewModel() = GroupCreationViewModel(ObserveAdminOrganization(accountRepository), repository)

    @Test
    fun `shows the policy of the organization and the limit`() {
        val state = viewModel().uiState.value

        assertEquals(GroupCreationPolicy.STAFF, state.policy)
        assertEquals(5, state.limit)
        assertFalse(state.isLoading)
    }

    @Test
    fun `saves the chosen policy`() {
        coEvery { repository.setPolicy(ORG, GroupCreationPolicy.EVERYONE) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onSelected(GroupCreationPolicy.EVERYONE)
        viewModel.onSaveClicked()

        coVerify { repository.setPolicy(ORG, GroupCreationPolicy.EVERYONE) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `keeps the page open when the server refuses`() {
        coEvery { repository.setPolicy(ORG, any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onSaveClicked()

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    private companion object {
        const val ORG = "org"
    }
}
