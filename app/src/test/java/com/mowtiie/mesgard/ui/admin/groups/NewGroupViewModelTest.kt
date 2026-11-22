package com.mowtiie.mesgard.ui.admin.groups

import com.mowtiie.mesgard.domain.admin.FindGroupCreationOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupCreationPolicy
import com.mowtiie.mesgard.domain.model.GroupCreationRules
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.model.GroupPlacement
import com.mowtiie.mesgard.domain.model.GroupSettings
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.PostingPolicy
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
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

class NewGroupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val role = MutableStateFlow(OrgRole.ORG_ADMIN)
    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } answers {
            MutableStateFlow(Account("Ana", null, listOf(AccountMembership(ORG, "Rizal National", role.value))))
        }
        coEvery { refresh() } returns RefreshResult.Success
    }
    private val groupCreationRepository = mockk<GroupCreationRepository> {
        coEvery { rules(ORG) } returns AdminResult.Success(
            GroupCreationRules(GroupCreationPolicy.EVERYONE, canCreate = true, created = 0, limit = 5),
        )
    }
    private val conversationRepository = mockk<ConversationRepository> {
        coEvery { refresh() } returns RefreshResult.Success
    }
    private val groupAdminRepository = mockk<GroupAdminRepository> {
        coEvery { listGroups(ORG) } returns AdminResult.Success(listOf(AdminGroup("g11", "Grade 11", 0)))
    }

    private fun viewModel() = NewGroupViewModel(
        FindGroupCreationOrganization(accountRepository, groupCreationRepository),
        groupAdminRepository,
        conversationRepository,
    )

    @Test
    fun `offers the groups of the organization as places for the new one`() {
        val viewModel = viewModel()

        viewModel.onParentChosen("g11")

        assertEquals("Grade 11", viewModel.uiState.value.parentName)
    }

    @Test
    fun `creates a group with its kind, its place and who can post`() {
        val settings = GroupSettings(PostingPolicy.MODERATORS, includesEveryone = true)
        val placement = GroupPlacement(GroupKind.SECTION, "g11")
        coEvery { groupAdminRepository.createGroup(ORG, "STEM", settings, placement) } returns
            AdminResult.Success("g12")
        val viewModel = viewModel()

        viewModel.onNameChanged("STEM")
        viewModel.onKindChanged(GroupKind.SECTION)
        viewModel.onParentChosen("g11")
        viewModel.onSettingsChanged(settings)
        viewModel.onCreateClicked()

        coVerify(exactly = 1) { groupAdminRepository.createGroup(ORG, "STEM", settings, placement) }
        coVerify(exactly = 1) { conversationRepository.refresh() }
        assertTrue(viewModel.uiState.value.isCreated)
    }

    @Test
    fun `does not create a group without a name`() {
        val viewModel = viewModel()

        viewModel.onNameChanged("   ")
        viewModel.onCreateClicked()

        coVerify(exactly = 0) { groupAdminRepository.createGroup(any(), any(), any(), any()) }
        assertFalse(viewModel.uiState.value.canCreate)
    }

    @Test
    fun `keeps the page open and shows the error when the server refuses`() {
        coEvery { groupAdminRepository.createGroup(ORG, "Chess", any(), any()) } returns
            AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onNameChanged("Chess")
        viewModel.onCreateClicked()

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isCreated)
    }

    @Test
    fun `a member makes a flat group without the places and settings of an administrator`() {
        role.value = OrgRole.MEMBER
        coEvery { groupAdminRepository.createGroup(ORG, "Chess", any(), GroupPlacement(GroupKind.CLUB)) } returns
            AdminResult.Success("g5")
        val viewModel = viewModel()
        viewModel.onNameChanged("Chess")
        viewModel.onKindChanged(GroupKind.CLUB)

        viewModel.onCreateClicked()

        assertFalse(viewModel.uiState.value.isAdmin)
        coVerify(exactly = 0) { groupAdminRepository.listGroups(any()) }
        assertTrue(viewModel.uiState.value.isCreated)
    }

    private companion object {
        const val ORG = "org"
    }
}
