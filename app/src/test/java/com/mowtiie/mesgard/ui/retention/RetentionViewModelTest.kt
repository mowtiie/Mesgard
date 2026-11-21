package com.mowtiie.mesgard.ui.retention

import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.RetentionRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RetentionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<RetentionRepository>()
    private val accountRepository = mockk<AccountRepository>()

    private fun viewModel(current: Int?): RetentionViewModel {
        every { accountRepository.observeAccount() } returns
            flowOf(Account("Ana", null, listOf(AccountMembership("org", "School", OrgRole.ORG_ADMIN))))
        coEvery { repository.retentionDays("org") } returns AdminResult.Success(current)
        return RetentionViewModel(ObserveAdminOrganization(accountRepository), repository)
    }

    @Test
    fun `a limit that is not set means messages are kept`() = runTest {
        val state = viewModel(current = null).uiState.value

        assertNull(state.selectedDays)
        assertEquals(RetentionPresets, state.options)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a preset that is set is chosen`() = runTest {
        assertEquals(730, viewModel(current = 730).uiState.value.selectedDays)
    }

    @Test
    fun `a limit the presets do not have is still shown and chosen`() = runTest {
        val state = viewModel(current = 30).uiState.value

        assertEquals(30, state.selectedDays)
        assertTrue(30 in state.options)
        assertEquals(RetentionPresets.size + 1, state.options.size)
    }

    @Test
    fun `saving sends the chosen limit and leaves the screen`() = runTest {
        coEvery { repository.setRetentionDays("org", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel(current = null)
        viewModel.onSelected(365)

        viewModel.onSaveClicked()

        coVerify { repository.setRetentionDays("org", 365) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `choosing no limit sends null`() = runTest {
        coEvery { repository.setRetentionDays("org", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel(current = 365)
        viewModel.onSelected(null)

        viewModel.onSaveClicked()

        coVerify { repository.setRetentionDays("org", null) }
    }

    @Test
    fun `a refused save is reported`() = runTest {
        coEvery { repository.setRetentionDays("org", any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel(current = null)

        viewModel.onSaveClicked()

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
