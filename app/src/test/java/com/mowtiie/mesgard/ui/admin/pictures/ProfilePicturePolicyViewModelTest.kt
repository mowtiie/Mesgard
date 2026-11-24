package com.mowtiie.mesgard.ui.admin.pictures

import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.ProfilePicturePolicy
import com.mowtiie.mesgard.domain.model.ProfilePictureRules
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ProfilePictureRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfilePicturePolicyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns
            flowOf(Account("Admin", null, listOf(AccountMembership("org1", "Rizal", OrgRole.ORG_ADMIN))))
    }
    private val repository = mockk<ProfilePictureRepository>()

    private fun viewModel(
        policy: ProfilePicturePolicy = ProfilePicturePolicy.STAFF_AND_ADMINS,
    ): ProfilePicturePolicyViewModel {
        coEvery { repository.rules("org1") } returns
            AdminResult.Success(ProfilePictureRules("org1", "u1", policy, true, null, null))
        return ProfilePicturePolicyViewModel(ObserveAdminOrganization(accountRepository), repository)
    }

    @Test
    fun `the page starts with the policy the organization has`() {
        val state = viewModel(ProfilePicturePolicy.OFF).uiState.value

        assertEquals(ProfilePicturePolicy.OFF, state.policy)
        assertFalse(state.isLoading)
    }

    @Test
    fun `saving sends the chosen policy and finishes`() {
        coEvery { repository.setPolicy("org1", ProfilePicturePolicy.EVERYONE) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onSelected(ProfilePicturePolicy.EVERYONE)
        viewModel.onSaveClicked()

        coVerify { repository.setPolicy("org1", ProfilePicturePolicy.EVERYONE) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `a refused change is reported and the page stays`() {
        coEvery { repository.setPolicy(any(), any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onSaveClicked()

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
