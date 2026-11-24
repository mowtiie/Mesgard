package com.mowtiie.mesgard.ui.admin

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.ErasureMode
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.RemovedMember
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AdminRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.UsernameRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MembersViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val account = MutableStateFlow<Account?>(
        Account("Admin", "admin@school.example", listOf(AccountMembership(ORG, "Rizal National", OrgRole.ORG_ADMIN))),
    )
    private val contacts = MutableStateFlow(
        listOf(
            Contact(ORG, "u1", "Ana Cruz", OrgRole.MEMBER),
            Contact(ORG, "u2", "Ben Reyes", OrgRole.STAFF),
            Contact("other-org", "u3", "Carl Diaz", OrgRole.MEMBER),
        ),
    )
    private val accountRepository = mockk<AccountRepository> { every { observeAccount() } returns account }
    private val contactRepository = mockk<ContactRepository> {
        every { observeContacts() } returns contacts
        coEvery { refresh() } returns RefreshResult.Success
    }
    private val usernameRepository = mockk<UsernameRepository>()
    private val adminRepository = mockk<AdminRepository> {
        coEvery { erasureMode() } returns AdminResult.Success(ErasureMode.KEEP_MESSAGES)
    }

    private fun viewModel() = MembersViewModel(
        ObserveAdminOrganization(accountRepository),
        contactRepository,
        adminRepository,
        usernameRepository,
    )

    @Test
    fun `lists the members of the administered organization only`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals("Rizal National", state.organizationName)
            assertEquals(listOf("Ana Cruz", "Ben Reyes"), state.members.map { it.displayName })
        }
    }

    @Test
    fun `searches the members by name`() = runTest {
        val viewModel = viewModel()

        viewModel.onQueryChanged("ben")

        viewModel.uiState.test {
            assertEquals(listOf("Ben Reyes"), expectMostRecentItem().members.map { it.displayName })
        }
    }

    @Test
    fun `reports a failed refresh`() = runTest {
        coEvery { contactRepository.refresh() } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        viewModel().uiState.test {
            assertEquals(MembersError.REFRESH_NETWORK_UNAVAILABLE, expectMostRecentItem().error)
        }
    }

    @Test
    fun `changes a role after the dialog is confirmed and refreshes the roster`() = runTest {
        coEvery { adminRepository.changeRole(ORG, "u1", OrgRole.STAFF) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[0], MemberAction.CHANGE_ROLE)
        viewModel.onRoleSelected(OrgRole.STAFF)
        viewModel.onDialogConfirmed()

        coVerify { adminRepository.changeRole(ORG, "u1", OrgRole.STAFF) }
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertNull(state.dialog)
            assertNull(state.error)
        }
        coVerify(atLeast = 2) { contactRepository.refresh() }
    }

    @Test
    fun `shows the temporary password after a reset`() = runTest {
        coEvery { adminRepository.resetPassword(ORG, "u1") } returns AdminResult.Success("Temp-1234")
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[0], MemberAction.RESET_PASSWORD)
        viewModel.onDialogConfirmed()

        viewModel.uiState.test {
            assertEquals(MembersDialog.TemporaryPassword("Ana Cruz", "Temp-1234"), expectMostRecentItem().dialog)
        }
        viewModel.onDialogDismissed()
        viewModel.uiState.test { assertNull(expectMostRecentItem().dialog) }
    }

    @Test
    fun `resets the two-step verification after the dialog is confirmed`() = runTest {
        coEvery { adminRepository.resetMfa(ORG, "u1") } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[0], MemberAction.RESET_MFA)
        viewModel.uiState.test {
            assertEquals(MembersDialog.ConfirmResetMfa(contacts.value[0]), expectMostRecentItem().dialog)
        }
        viewModel.onDialogConfirmed()

        coVerify { adminRepository.resetMfa(ORG, "u1") }
        viewModel.uiState.test { assertNull(expectMostRecentItem().dialog) }
    }

    @Test
    fun `resets the username after the dialog is confirmed`() = runTest {
        coEvery { usernameRepository.reset(ORG, "u1") } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[0], MemberAction.RESET_USERNAME)
        viewModel.uiState.test {
            assertEquals(MembersDialog.ConfirmResetUsername(contacts.value[0]), expectMostRecentItem().dialog)
        }
        viewModel.onDialogConfirmed()

        coVerify { usernameRepository.reset(ORG, "u1") }
        viewModel.uiState.test { assertNull(expectMostRecentItem().dialog) }
    }

    @Test
    fun `keeps the dialog and shows the error when the reset is refused`() = runTest {
        coEvery { adminRepository.resetMfa(ORG, "u1") } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[0], MemberAction.RESET_MFA)
        viewModel.onDialogConfirmed()

        viewModel.uiState.test { assertEquals(MembersError.NOT_ALLOWED, expectMostRecentItem().error) }
    }

    @Test
    fun `offers erasing only when the server allows it`() = runTest {
        viewModel().uiState.test { assertEquals(ErasureMode.KEEP_MESSAGES, expectMostRecentItem().erasureMode) }

        coEvery { adminRepository.erasureMode() } returns AdminResult.Failure(AdminError.NOT_A_MEMBER)
        viewModel().uiState.test { assertEquals(ErasureMode.OFF, expectMostRecentItem().erasureMode) }
    }

    @Test
    fun `erases instead of only removing when the box is ticked`() = runTest {
        coEvery { adminRepository.eraseMember(ORG, "u2") } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[1], MemberAction.REMOVE)
        viewModel.uiState.test {
            assertEquals(MembersDialog.ConfirmRemove(contacts.value[1], erase = false), expectMostRecentItem().dialog)
        }
        viewModel.onEraseToggled(true)
        viewModel.uiState.test {
            assertEquals(MembersDialog.ConfirmRemove(contacts.value[1], erase = true), expectMostRecentItem().dialog)
        }
        viewModel.onDialogConfirmed()

        coVerify(exactly = 1) { adminRepository.eraseMember(ORG, "u2") }
        coVerify(exactly = 0) { adminRepository.removeMember(any(), any()) }
        viewModel.uiState.test { assertNull(expectMostRecentItem().dialog) }
    }

    @Test
    fun `says so when the person cannot be erased`() = runTest {
        coEvery { adminRepository.eraseMember(ORG, "u1") } returns AdminResult.Failure(AdminError.NOT_ERASABLE)
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[0], MemberAction.REMOVE)
        viewModel.onEraseToggled(true)
        viewModel.onDialogConfirmed()

        viewModel.uiState.test { assertEquals(MembersError.NOT_ERASABLE, expectMostRecentItem().error) }
    }

    @Test
    fun `removes a member after the dialog is confirmed`() = runTest {
        coEvery { adminRepository.removeMember(ORG, "u2") } returns AdminResult.Success(RemovedMember(true))
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[1], MemberAction.REMOVE)
        viewModel.onDialogConfirmed()

        coVerify { adminRepository.removeMember(ORG, "u2") }
        viewModel.uiState.test { assertNull(expectMostRecentItem().dialog) }
    }

    @Test
    fun `keeps the roster untouched and reports the error when the server refuses`() = runTest {
        coEvery { adminRepository.removeMember(ORG, "u2") } returns AdminResult.Failure(AdminError.LAST_ADMIN)
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[1], MemberAction.REMOVE)
        viewModel.onDialogConfirmed()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(MembersError.LAST_ADMIN, state.error)
            assertNull(state.dialog)
        }
        coVerify(exactly = 1) { contactRepository.refresh() }
        viewModel.onErrorShown()
        viewModel.uiState.test { assertNull(expectMostRecentItem().error) }
    }

    @Test
    fun `a person who is not an admin cannot confirm an action`() = runTest {
        account.value = Account("Ana", null, listOf(AccountMembership(ORG, "Rizal National", OrgRole.MEMBER)))
        val viewModel = viewModel()

        viewModel.onMemberAction(contacts.value[0], MemberAction.REMOVE)
        viewModel.onDialogConfirmed()

        viewModel.uiState.test { assertEquals(MembersError.NOT_ALLOWED, expectMostRecentItem().error) }
        coVerify(exactly = 0) { adminRepository.removeMember(any(), any()) }
    }

    private companion object {
        const val ORG = "org"
    }
}
