package com.mowtiie.mesgard.ui.admin

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MemberCreation
import com.mowtiie.mesgard.domain.model.NewMember
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.SkipReason
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AdminRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
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

class AddMemberViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns MutableStateFlow(
            Account("Admin", null, listOf(AccountMembership(ORG, "Rizal National", OrgRole.ORG_ADMIN))),
        )
    }
    private val adminRepository = mockk<AdminRepository>()
    private val contactRepository = mockk<ContactRepository> { coEvery { refresh() } returns RefreshResult.Success }

    private fun viewModel() =
        AddMemberViewModel(ObserveAdminOrganization(accountRepository), adminRepository, contactRepository)

    @Test
    fun `cannot add without an email or while saving`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEmailChanged("ana@school.example")

        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `adds a member with the chosen role and shows the temporary password`() = runTest {
        coEvery { adminRepository.createMembers(ORG, any()) } returns
            AdminResult.Success(listOf(MemberCreation.Created("ana@school.example", "u1", "Temp-1234")))
        val viewModel = viewModel()
        viewModel.onEmailChanged(" ana@school.example ")
        viewModel.onDisplayNameChanged(" Ana Cruz ")
        viewModel.onRoleSelected(OrgRole.STAFF)

        viewModel.onAddClicked()

        coVerify {
            adminRepository.createMembers(ORG, listOf(NewMember("ana@school.example", "Ana Cruz", OrgRole.STAFF)))
        }
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(CreatedLogin("Ana Cruz", "Temp-1234"), state.created)
            assertNull(state.error)
        }
        coVerify { contactRepository.refresh() }
        viewModel.onCreatedHandled()
        viewModel.uiState.test { assertNull(expectMostRecentItem().created) }
    }

    @Test
    fun `falls back to the email when no name was given`() = runTest {
        coEvery { adminRepository.createMembers(ORG, any()) } returns
            AdminResult.Success(listOf(MemberCreation.Created("ana@school.example", "u1", "Temp-1234")))
        val viewModel = viewModel()
        viewModel.onEmailChanged("ana@school.example")

        viewModel.onAddClicked()

        viewModel.uiState.test { assertEquals("ana@school.example", expectMostRecentItem().created?.displayName) }
    }

    @Test
    fun `reports an email that already has an account without creating anything`() = runTest {
        coEvery { adminRepository.createMembers(ORG, any()) } returns
            AdminResult.Success(listOf(MemberCreation.Skipped("ana@school.example", SkipReason.EMAIL_TAKEN)))
        val viewModel = viewModel()
        viewModel.onEmailChanged("ana@school.example")

        viewModel.onAddClicked()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(AddMemberError.EMAIL_TAKEN, state.error)
            assertTrue(state.isEmailError)
            assertNull(state.created)
        }
        coVerify(exactly = 0) { contactRepository.refresh() }
    }

    @Test
    fun `reports a refusal by the server`() = runTest {
        coEvery { adminRepository.createMembers(ORG, any()) } returns
            AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        val viewModel = viewModel()
        viewModel.onEmailChanged("ana@school.example")

        viewModel.onAddClicked()

        viewModel.uiState.test { assertEquals(AddMemberError.NETWORK_UNAVAILABLE, expectMostRecentItem().error) }
        viewModel.onErrorShown()
        viewModel.uiState.test { assertNull(expectMostRecentItem().error) }
    }

    private companion object {
        const val ORG = "org"
    }
}
