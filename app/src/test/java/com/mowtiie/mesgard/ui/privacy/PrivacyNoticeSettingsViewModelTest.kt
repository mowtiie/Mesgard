package com.mowtiie.mesgard.ui.privacy

import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.PrivacyNotice
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.PrivacyNoticeRepository
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

class PrivacyNoticeSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<PrivacyNoticeRepository>()
    private val accountRepository = mockk<AccountRepository>()

    private fun viewModel(): PrivacyNoticeSettingsViewModel {
        every { accountRepository.observeAccount() } returns
            flowOf(Account("Ana", null, listOf(AccountMembership("org", "School", OrgRole.ORG_ADMIN))))
        coEvery { repository.notices() } returns AdminResult.Success(
            listOf(PrivacyNotice("org", "School", "https://school.example/privacy", null, isAcknowledged = true)),
        )
        return PrivacyNoticeSettingsViewModel(ObserveAdminOrganization(accountRepository), repository)
    }

    @Test
    fun `the links already set are shown`() = runTest {
        val state = viewModel().uiState.value

        assertEquals("https://school.example/privacy", state.noticeUrl)
        assertEquals("", state.childNoticeUrl)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a link that is not an address cannot be saved`() = runTest {
        val viewModel = viewModel()

        viewModel.onChildNoticeUrlChanged("javascript:alert(1)")

        assertFalse(viewModel.uiState.value.isChildNoticeUrlValid)
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onSaveClicked()
        coVerify(exactly = 0) { repository.setLinks(any(), any(), any()) }
    }

    @Test
    fun `saving sends both links to the organization and leaves the screen`() = runTest {
        coEvery { repository.setLinks("org", any(), any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onChildNoticeUrlChanged("http://intranet.local/kids")

        viewModel.onSaveClicked()

        coVerify { repository.setLinks("org", "https://school.example/privacy", "http://intranet.local/kids") }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `a refused save is reported`() = runTest {
        coEvery { repository.setLinks("org", any(), any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onSaveClicked()

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
