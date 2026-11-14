package com.mowtiie.mesgard.ui.admin

import com.mowtiie.mesgard.domain.admin.DocumentStore
import com.mowtiie.mesgard.domain.admin.IssuedCredential
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.admin.RosterFileProblem
import com.mowtiie.mesgard.domain.admin.RosterProblem
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
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImportMembersViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns MutableStateFlow(
            Account("Admin", null, listOf(AccountMembership(ORG, "Rizal National", OrgRole.ORG_ADMIN))),
        )
    }
    private val adminRepository = mockk<AdminRepository>()
    private val contactRepository = mockk<ContactRepository> { coEvery { refresh() } returns RefreshResult.Success }
    private val documentStore = mockk<DocumentStore>()

    private fun viewModel() = ImportMembersViewModel(
        ObserveAdminOrganization(accountRepository),
        adminRepository,
        contactRepository,
        documentStore,
    )

    private fun file(text: String?) {
        coEvery { documentStore.readText(FILE, any()) } returns text
    }

    @Test
    fun `shows a preview that separates ready rows from problems`() {
        file("email,name,role\nana@school.example,Ana,student\nbad,Bad,student\n")
        val viewModel = viewModel()

        viewModel.onFileChosen(FILE)

        val state = viewModel.uiState.value
        assertEquals(ImportStage.PREVIEW, state.stage)
        assertEquals(listOf("ana@school.example"), state.readyEntries.map { it.email })
        assertEquals(listOf(RosterProblem.INVALID_EMAIL), state.problemEntries.map { it.problem })
    }

    @Test
    fun `stays on the start screen when the file cannot be used`() {
        val viewModel = viewModel()

        file(null)
        viewModel.onFileChosen(FILE)
        assertEquals(RosterFileProblem.UNREADABLE, viewModel.uiState.value.fileProblem)

        file("name\nAna\n")
        viewModel.onFileChosen(FILE)
        assertEquals(RosterFileProblem.NO_EMAIL_COLUMN, viewModel.uiState.value.fileProblem)
        assertEquals(ImportStage.START, viewModel.uiState.value.stage)
    }

    @Test
    fun `imports the ready rows in batches and lists the credentials`() {
        val rows = (1..120).joinToString("\n") { "u$it@school.example,User $it,student" }
        file("email,name,role\n$rows\n")
        val batches = mutableListOf<List<NewMember>>()
        coEvery { adminRepository.createMembers(ORG, capture(batches)) } answers {
            AdminResult.Success(
                batches.last().map {
                    MemberCreation.Created(it.email, "id-${it.email}", "pw-${it.email}")
                },
            )
        }
        val viewModel = viewModel()
        viewModel.onFileChosen(FILE)

        viewModel.onImportClicked()

        assertEquals(listOf(50, 50, 20), batches.map { it.size })
        val state = viewModel.uiState.value
        assertEquals(ImportStage.DONE, state.stage)
        assertNull(state.interruptedBy)
        assertEquals(120, state.credentials.size)
        assertEquals(IssuedCredential("u1@school.example", "User 1", "pw-u1@school.example"), state.credentials.first())
        coVerify { contactRepository.refresh() }
    }

    @Test
    fun `keeps the people created so far when a batch fails`() {
        val rows = (1..60).joinToString("\n") { "u$it@school.example" }
        file("email\n$rows\n")
        val calls = slot<List<NewMember>>()
        var call = 0
        coEvery { adminRepository.createMembers(ORG, capture(calls)) } answers {
            if (call++ == 0) {
                AdminResult.Success(calls.captured.map { MemberCreation.Created(it.email, "id", "pw") })
            } else {
                AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
            }
        }
        val viewModel = viewModel()
        viewModel.onFileChosen(FILE)

        viewModel.onImportClicked()

        val state = viewModel.uiState.value
        assertEquals(ImportStage.DONE, state.stage)
        assertEquals(AdminError.NETWORK_UNAVAILABLE, state.interruptedBy)
        assertEquals(50, state.credentials.size)
    }

    @Test
    fun `lists the people the server skipped`() {
        file("email\nana@school.example\nben@school.example\n")
        coEvery { adminRepository.createMembers(ORG, any()) } returns AdminResult.Success(
            listOf(
                MemberCreation.Created("ana@school.example", "u1", "pw"),
                MemberCreation.Skipped("ben@school.example", SkipReason.EMAIL_TAKEN),
            ),
        )
        val viewModel = viewModel()
        viewModel.onFileChosen(FILE)

        viewModel.onImportClicked()

        assertEquals(listOf("ben@school.example"), viewModel.uiState.value.skipped.map { it.email })
    }

    @Test
    fun `does nothing when no row is ready`() {
        file("email\nnot-an-email\n")
        val viewModel = viewModel()
        viewModel.onFileChosen(FILE)

        viewModel.onImportClicked()

        assertEquals(ImportStage.PREVIEW, viewModel.uiState.value.stage)
        coVerify(exactly = 0) { adminRepository.createMembers(any(), any()) }
    }

    @Test
    fun `warns before leaving with passwords that were not saved`() {
        file("email\nana@school.example\n")
        coEvery { adminRepository.createMembers(ORG, any()) } returns
            AdminResult.Success(listOf(MemberCreation.Created("ana@school.example", "u1", "pw")))
        coEvery { documentStore.writeText(DESTINATION, any()) } returns true
        val viewModel = viewModel()
        viewModel.onFileChosen(FILE)
        viewModel.onImportClicked()
        assertTrue(viewModel.uiState.value.needsLeaveWarning)

        viewModel.onCredentialsDestinationChosen(DESTINATION)

        assertFalse(viewModel.uiState.value.needsLeaveWarning)
        assertEquals(ImportMessage.SAVED, viewModel.uiState.value.message)
        coVerify {
            documentStore.writeText(
                DESTINATION,
                "email,name,temporary_password\nana@school.example,ana,pw\n",
            )
        }
    }

    @Test
    fun `copying the passwords counts as saving them and a failed save does not`() {
        file("email\nana@school.example\n")
        coEvery { adminRepository.createMembers(ORG, any()) } returns
            AdminResult.Success(listOf(MemberCreation.Created("ana@school.example", "u1", "pw")))
        coEvery { documentStore.writeText(DESTINATION, any()) } returns false
        val viewModel = viewModel()
        viewModel.onFileChosen(FILE)
        viewModel.onImportClicked()

        viewModel.onCredentialsDestinationChosen(DESTINATION)
        assertEquals(ImportMessage.SAVE_FAILED, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.needsLeaveWarning)

        viewModel.onCredentialsCopied()
        assertFalse(viewModel.uiState.value.needsLeaveWarning)
    }

    @Test
    fun `saves the template to the chosen file`() {
        coEvery { documentStore.writeText(DESTINATION, any()) } returns true
        val viewModel = viewModel()

        viewModel.onTemplateDestinationChosen(DESTINATION)

        coVerify { documentStore.writeText(DESTINATION, match { it.startsWith("email,name,role") }) }
        viewModel.onMessageShown()
        assertNull(viewModel.uiState.value.message)
    }

    private companion object {
        const val ORG = "org"
        const val FILE = "content://files/roster.csv"
        const val DESTINATION = "content://files/out.csv"
    }
}
