package com.mowtiie.mesgard.ui.admin.groups

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.GroupDetail
import com.mowtiie.mesgard.domain.model.GroupMember
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class GroupDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contacts = MutableStateFlow(
        listOf(
            Contact("org", "u1", "Ana Cruz", OrgRole.STUDENT),
            Contact("org", "u2", "Ben Reyes", OrgRole.STAFF),
        ),
    )
    private val contactRepository = mockk<ContactRepository> { every { observeContacts() } returns contacts }
    private val groupAdminRepository = mockk<GroupAdminRepository>()
    private val detail = GroupDetail(
        "g1",
        "Grade 10 A",
        listOf(
            GroupMember("u1", GroupRole.MEMBER),
            GroupMember("u2", GroupRole.MODERATOR),
            GroupMember("u9", GroupRole.MEMBER),
        ),
    )

    private fun viewModel() = GroupDetailViewModel(
        SavedStateHandle(mapOf("groupId" to "g1")),
        contactRepository,
        groupAdminRepository,
    )

    @Test
    fun `shows the group with moderators first and names from the roster`() = runTest {
        coEvery { groupAdminRepository.fetchGroup("g1") } returns AdminResult.Success(detail)

        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals("Grade 10 A", state.name)
            assertEquals(listOf("Ben Reyes", null, "Ana Cruz"), state.members.map { it.displayName })
            assertEquals(GroupRole.MODERATOR, state.members.first().role)
        }
    }

    @Test
    fun `renames the group and loads it again`() = runTest {
        coEvery { groupAdminRepository.fetchGroup("g1") } returns AdminResult.Success(detail)
        coEvery { groupAdminRepository.renameGroup("g1", "Grade 10 B") } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onDialogRequested(GroupDetailDialog.Rename)
        viewModel.onRenameConfirmed("Grade 10 B")

        coVerify { groupAdminRepository.renameGroup("g1", "Grade 10 B") }
        coVerify(exactly = 2) { groupAdminRepository.fetchGroup("g1") }
        viewModel.uiState.test { assertNull(expectMostRecentItem().dialog) }
    }

    @Test
    fun `deleting the group marks it as deleted without loading it again`() = runTest {
        coEvery { groupAdminRepository.fetchGroup("g1") } returns AdminResult.Success(detail)
        coEvery { groupAdminRepository.deleteGroup("g1") } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onDeleteConfirmed()

        viewModel.uiState.test { assertTrue(expectMostRecentItem().isDeleted) }
        coVerify(exactly = 1) { groupAdminRepository.fetchGroup("g1") }
    }

    @Test
    fun `switches a member between moderator and member`() = runTest {
        coEvery { groupAdminRepository.fetchGroup("g1") } returns AdminResult.Success(detail)
        coEvery { groupAdminRepository.changeMemberRole("g1", any(), any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        val rows = viewModel.uiState.first { it.members.isNotEmpty() }.members

        viewModel.onRoleToggled(rows.first { it.userId == "u1" })
        viewModel.onRoleToggled(rows.first { it.userId == "u2" })

        coVerify { groupAdminRepository.changeMemberRole("g1", "u1", GroupRole.MODERATOR) }
        coVerify { groupAdminRepository.changeMemberRole("g1", "u2", GroupRole.MEMBER) }
    }

    @Test
    fun `removes a member after confirmation`() = runTest {
        coEvery { groupAdminRepository.fetchGroup("g1") } returns AdminResult.Success(detail)
        coEvery { groupAdminRepository.removeMember("g1", "u1") } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        val rows = viewModel.uiState.first { it.members.isNotEmpty() }.members
        viewModel.onRemoveConfirmed(rows.first { it.userId == "u1" })

        coVerify { groupAdminRepository.removeMember("g1", "u1") }
    }

    @Test
    fun `reports a refusal and keeps the group`() = runTest {
        coEvery { groupAdminRepository.fetchGroup("g1") } returns AdminResult.Success(detail)
        coEvery { groupAdminRepository.deleteGroup("g1") } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onDialogRequested(GroupDetailDialog.ConfirmDelete)
        viewModel.onDeleteConfirmed()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(AdminError.NOT_ALLOWED, state.error)
            assertFalse(state.isDeleted)
            assertNull(state.dialog)
        }
    }
}
