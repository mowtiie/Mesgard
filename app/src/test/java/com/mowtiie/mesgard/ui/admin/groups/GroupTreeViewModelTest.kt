package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupKind
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.domain.repository.GroupAnnouncementRepository
import com.mowtiie.mesgard.domain.repository.GroupTreeRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupTreeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns MutableStateFlow(
            Account("Admin", null, listOf(AccountMembership(ORG, "Rizal National", OrgRole.ORG_ADMIN))),
        )
    }
    private val groups = listOf(
        AdminGroup("g11", "Grade 11", 0, GroupKind.CLASS),
        AdminGroup("stem", "STEM", 0, GroupKind.SECTION, "g11"),
        AdminGroup("lab", "Lab", 0, GroupKind.OTHER, "stem"),
    )
    private val groupAdminRepository = mockk<GroupAdminRepository> {
        coEvery { listGroups(ORG) } returns AdminResult.Success(groups)
    }
    private val groupTreeRepository = mockk<GroupTreeRepository>()
    private val groupAnnouncementRepository = mockk<GroupAnnouncementRepository>()

    private fun viewModel() = GroupTreeViewModel(
        SavedStateHandle(mapOf("groupId" to "stem")),
        ObserveAdminOrganization(accountRepository),
        groupAdminRepository,
        groupTreeRepository,
        groupAnnouncementRepository,
    )

    @Test
    fun `knows the kind and the parent of the group`() {
        val state = viewModel().uiState.value

        assertEquals(GroupKind.SECTION, state.kind)
        assertEquals("Grade 11", state.parentName)
    }

    @Test
    fun `a group cannot go inside itself or inside a group it contains`() {
        assertEquals(setOf("stem", "lab"), viewModel().uiState.value.excludedParentIds)
    }

    @Test
    fun `moves the group, closes the sheet and reads the tree again`() {
        coEvery { groupTreeRepository.moveGroup("stem", null) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onDialogChanged(GroupTreeDialog.PARENT)

        viewModel.onParentChosen(null)

        coVerify { groupTreeRepository.moveGroup("stem", null) }
        coVerify(exactly = 2) { groupAdminRepository.listGroups(ORG) }
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `keeps the error when the server refuses the new kind`() {
        coEvery { groupTreeRepository.changeKind("stem", GroupKind.CLUB) } returns
            AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onKindChosen(GroupKind.CLUB)

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `knows whether there are groups inside`() {
        assertEquals(true, viewModel().uiState.value.hasGroupsInside)
    }

    @Test
    fun `sends the announcement once, closes the sheet and reports how many conversations it reached`() {
        coEvery { groupAnnouncementRepository.send("stem", any(), "Exams start Monday") } returns AdminResult.Success(2)
        val viewModel = viewModel()
        viewModel.onDialogChanged(GroupTreeDialog.ANNOUNCE)

        viewModel.onAnnounceConfirmed("Exams start Monday")

        coVerify(exactly = 1) { groupAnnouncementRepository.send("stem", any(), "Exams start Monday") }
        assertEquals(2, viewModel.uiState.value.announcedTo)
        assertNull(viewModel.uiState.value.dialog)
        viewModel.onAnnouncementShown()
        assertNull(viewModel.uiState.value.announcedTo)
    }

    @Test
    fun `does not send an empty announcement and keeps the error of a refused one`() {
        coEvery { groupAnnouncementRepository.send("stem", any(), "Hello") } returns
            AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onAnnounceConfirmed("   ")
        coVerify(exactly = 0) { groupAnnouncementRepository.send(any(), any(), any()) }

        viewModel.onAnnounceConfirmed("Hello")
        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.announcedTo)
    }

    private companion object {
        const val ORG = "org"
    }
}
