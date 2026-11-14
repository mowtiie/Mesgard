package com.mowtiie.mesgard.ui.settings

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.admin.ObservePostingOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AnnouncementRepository
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

class AnnouncementsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val account = MutableStateFlow<Account?>(accountWith(OrgRole.STAFF))
    private val accountRepository = mockk<AccountRepository> { every { observeAccount() } returns account }
    private val announcementRepository = mockk<AnnouncementRepository>()

    private fun accountWith(role: OrgRole) =
        Account("Person", null, listOf(AccountMembership(ORG, "Rizal National", role)))

    private fun viewModel() =
        AnnouncementsViewModel(ObservePostingOrganization(accountRepository), announcementRepository)

    @Test
    fun `admins and staff can post and students cannot`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test { assertTrue(expectMostRecentItem().canPost) }

        account.value = accountWith(OrgRole.ORG_ADMIN)
        viewModel.uiState.test { assertTrue(expectMostRecentItem().canPost) }

        account.value = accountWith(OrgRole.STUDENT)
        viewModel.uiState.test { assertFalse(expectMostRecentItem().canPost) }
    }

    @Test
    fun `opens the announcement conversation of the organization`() = runTest {
        coEvery { announcementRepository.openAnnouncements(ORG) } returns AdminResult.Success("conversation-1")
        val viewModel = viewModel()

        viewModel.onOpenClicked()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("conversation-1", state.conversationToOpen)
            assertFalse(state.isOpening)
        }
        viewModel.onOpened()
        viewModel.uiState.test { assertNull(expectMostRecentItem().conversationToOpen) }
    }

    @Test
    fun `reports why the conversation could not be opened`() = runTest {
        coEvery { announcementRepository.openAnnouncements(ORG) } returns
            AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        val viewModel = viewModel()

        viewModel.onOpenClicked()

        viewModel.uiState.test { assertEquals(AdminError.NETWORK_UNAVAILABLE, expectMostRecentItem().error) }
        viewModel.onErrorShown()
        viewModel.uiState.test { assertNull(expectMostRecentItem().error) }
    }

    @Test
    fun `a student cannot open it`() = runTest {
        account.value = accountWith(OrgRole.STUDENT)
        val viewModel = viewModel()

        viewModel.onOpenClicked()

        viewModel.uiState.test { assertEquals(AdminError.NOT_ALLOWED, expectMostRecentItem().error) }
        coVerify(exactly = 0) { announcementRepository.openAnnouncements(any()) }
    }

    private companion object {
        const val ORG = "org"
    }
}
