package com.mowtiie.mesgard.ui.admin.groups

import com.mowtiie.mesgard.domain.admin.DocumentStore
import com.mowtiie.mesgard.domain.admin.GroupFileProblem
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupImportResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.GroupImportRepository
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

class ImportGroupsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns MutableStateFlow(
            Account("Admin", null, listOf(AccountMembership(ORG, "Rizal National", OrgRole.ORG_ADMIN))),
        )
    }
    private val repository = mockk<GroupImportRepository>()
    private val documents = mockk<DocumentStore>()

    private fun viewModel() = ImportGroupsViewModel(ObserveAdminOrganization(accountRepository), repository, documents)

    private fun file(text: String?) {
        coEvery { documents.readText("file", any()) } returns text
    }

    @Test
    fun `shows what a good file holds before anything is sent`() {
        file("group,parent,email\nGrade 11,,\nSTEM,Grade 11,ana@school.example\nSTEM,Grade 11,ben@school.example\n")
        val viewModel = viewModel()

        viewModel.onFileChosen("file")

        val state = viewModel.uiState.value
        assertEquals(ImportGroupsStage.PREVIEW, state.stage)
        assertEquals(2, state.groupCount)
        assertEquals(2, state.peopleCount)
        assertEquals(0, state.problemRows.size)
    }

    @Test
    fun `lists the rows that will be skipped and sends only the others`() {
        file("group,kind\nChess,club\n,club\nRobotics,house\n")
        coEvery { repository.import(ORG, any()) } returns AdminResult.Success(GroupImportResult(1, 0, emptyList()))
        val viewModel = viewModel()

        viewModel.onFileChosen("file")
        assertEquals(2, viewModel.uiState.value.problemRows.size)
        viewModel.onImportClicked()

        coVerify { repository.import(ORG, match { rows -> rows.map { it.group } == listOf("Chess") }) }
        assertEquals(ImportGroupsStage.DONE, viewModel.uiState.value.stage)
    }

    @Test
    fun `a file that cannot be read or has no group column stays on the first step`() {
        file(null)
        val viewModel = viewModel()
        viewModel.onFileChosen("file")
        assertEquals(GroupFileProblem.UNREADABLE, viewModel.uiState.value.fileProblem)

        file("email\nana@school.example\n")
        viewModel.onFileChosen("file")
        assertEquals(GroupFileProblem.NO_GROUP_COLUMN, viewModel.uiState.value.fileProblem)
        assertEquals(ImportGroupsStage.START, viewModel.uiState.value.stage)
    }

    @Test
    fun `goes back to the preview with the error when the server refuses`() {
        file("group\nChess\n")
        coEvery { repository.import(ORG, any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onFileChosen("file")
        viewModel.onImportClicked()

        assertEquals(ImportGroupsStage.PREVIEW, viewModel.uiState.value.stage)
        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }

    private companion object {
        const val ORG = "org"
    }
}
