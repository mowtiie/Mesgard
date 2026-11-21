package com.mowtiie.mesgard.ui.reports

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.ReportReviewer
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReportReviewersViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ReportReviewRepository>()
    private val contactRepository = mockk<ContactRepository>()
    private val accountRepository = mockk<AccountRepository>()

    private fun contact(id: String, role: OrgRole) = Contact("org", id, "Person $id", role)

    private fun viewModel(chosen: List<String> = emptyList()): ReportReviewersViewModel {
        every { accountRepository.observeAccount() } returns
            flowOf(Account("Ana", null, listOf(AccountMembership("org", "School", OrgRole.ORG_ADMIN))))
        every { contactRepository.observeContacts() } returns flowOf(
            listOf(
                contact("a1", OrgRole.ORG_ADMIN),
                contact("s1", OrgRole.STAFF),
                contact("m1", OrgRole.MEMBER),
            ),
        )
        coEvery { repository.reviewers("org") } returns
            AdminResult.Success(chosen.map { ReportReviewer(it, "Person $it") })
        return ReportReviewersViewModel(ObserveAdminOrganization(accountRepository), repository, contactRepository)
    }

    @Test
    fun `only administrators and staff can be chosen`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("a1", "s1"), state.candidates.map { it.userId })
        }
    }

    @Test
    fun `the reviewers already chosen are ticked`() = runTest {
        val viewModel = viewModel(chosen = listOf("s1"))

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(setOf("s1"), state.selectedIds)
            assertFalse(state.adminsReview)
        }
    }

    @Test
    fun `with nobody chosen the administrators review`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test { assertTrue(expectMostRecentItem().adminsReview) }
    }

    @Test
    fun `saving sends the chosen people and leaves the screen`() = runTest {
        coEvery { repository.setReviewers("org", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onToggled("s1")
            viewModel.onToggled("a1")
            viewModel.onToggled("a1")

            viewModel.onSaveClicked()

            assertTrue(expectMostRecentItem().isSaved)
        }
        coVerify { repository.setReviewers("org", listOf("s1")) }
    }

    @Test
    fun `a refused save is reported`() = runTest {
        coEvery { repository.setReviewers("org", any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()
        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSaveClicked()

            val state = expectMostRecentItem()
            assertEquals(AdminError.NOT_ALLOWED, state.error)
            assertFalse(state.isSaved)
        }
    }
}
