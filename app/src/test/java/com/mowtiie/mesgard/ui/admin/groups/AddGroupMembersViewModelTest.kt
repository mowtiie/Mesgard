package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.GroupDetail
import com.mowtiie.mesgard.domain.model.GroupMember
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddGroupMembersViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns MutableStateFlow(
            Account("Admin", null, listOf(AccountMembership(ORG, "Rizal National", OrgRole.ORG_ADMIN))),
        )
    }
    private val contactRepository = mockk<ContactRepository> {
        every { observeContacts() } returns MutableStateFlow(
            listOf(
                Contact(ORG, "u1", "Ana Cruz", OrgRole.MEMBER),
                Contact(ORG, "u2", "Ben Reyes", OrgRole.STAFF),
                Contact(ORG, "u3", "Cy Diaz", OrgRole.MEMBER),
                Contact("other-org", "u4", "Dee Lim", OrgRole.MEMBER),
            ),
        )
    }
    private val groupAdminRepository = mockk<GroupAdminRepository> {
        coEvery { fetchGroup("g1") } returns AdminResult.Success(
            GroupDetail("g1", "Grade 10 A", listOf(GroupMember("u1", GroupRole.MEMBER))),
        )
    }

    private fun viewModel() = AddGroupMembersViewModel(
        SavedStateHandle(mapOf("groupId" to "g1")),
        ObserveAdminOrganization(accountRepository),
        contactRepository,
        groupAdminRepository,
    )

    @Test
    fun `offers the members of the organization who are not in the group`() = runTest {
        viewModel().uiState.test {
            assertEquals(listOf("Ben Reyes", "Cy Diaz"), expectMostRecentItem().candidates.map { it.displayName })
        }
    }

    @Test
    fun `searches the candidates and keeps the selection`() = runTest {
        val viewModel = viewModel()

        viewModel.onToggled("u2")
        viewModel.onQueryChanged("cy")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("Cy Diaz"), state.candidates.map { it.displayName })
            assertEquals(setOf("u2"), state.selectedIds)
        }
    }

    @Test
    fun `selects and unselects a person and needs a selection to add`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test { assertFalse(expectMostRecentItem().canAdd) }

        viewModel.onToggled("u2")
        viewModel.uiState.test { assertTrue(expectMostRecentItem().canAdd) }

        viewModel.onToggled("u2")
        viewModel.uiState.test { assertFalse(expectMostRecentItem().canAdd) }
    }

    @Test
    fun `adds the selected people and finishes`() = runTest {
        coEvery { groupAdminRepository.addMembers(ORG, "g1", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onToggled("u2")
        viewModel.onToggled("u3")

        viewModel.onAddClicked()

        coVerify { groupAdminRepository.addMembers(ORG, "g1", listOf("u2", "u3")) }
        viewModel.uiState.test { assertTrue(expectMostRecentItem().isDone) }
    }

    @Test
    fun `stays on the screen and reports the error when the server refuses`() = runTest {
        coEvery { groupAdminRepository.addMembers(ORG, "g1", any()) } returns
            AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()
        viewModel.onToggled("u2")

        viewModel.onAddClicked()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(AdminError.NOT_ALLOWED, state.error)
            assertFalse(state.isDone)
        }
        viewModel.onErrorShown()
        viewModel.uiState.test { assertNull(expectMostRecentItem().error) }
    }

    private companion object {
        const val ORG = "org"
    }
}
