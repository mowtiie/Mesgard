package com.mowtiie.mesgard.ui.messaging

import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MessagingRules
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.MessagingRulesRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MessagingRulesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<MessagingRulesRepository>()
    private val accountRepository = mockk<AccountRepository>()

    private fun viewModel(current: MessagingRules = MessagingRules()): MessagingRulesViewModel {
        every { accountRepository.observeAccount() } returns
            flowOf(Account("Ana", null, listOf(AccountMembership("org", "School", OrgRole.ORG_ADMIN))))
        coEvery { repository.rules("org") } returns AdminResult.Success(current)
        return MessagingRulesViewModel(ObserveAdminOrganization(accountRepository), repository)
    }

    @Test
    fun `the rules of the organization are shown`() = runTest {
        val rules = MessagingRules(true, LocalTime.of(20, 0), LocalTime.of(5, 30))

        val state = viewModel(rules).uiState.value

        assertEquals(rules, state.rules)
        assertFalse(state.isLoading)
    }

    @Test
    fun `quiet hours start from nine in the evening to six in the morning`() = runTest {
        val viewModel = viewModel()

        viewModel.onQuietHoursChanged(true)

        assertEquals(LocalTime.of(21, 0), viewModel.uiState.value.rules.quietStart)
        assertEquals(LocalTime.of(6, 0), viewModel.uiState.value.rules.quietEnd)
    }

    @Test
    fun `turning quiet hours off removes the window`() = runTest {
        val viewModel = viewModel(MessagingRules(false, LocalTime.of(21, 0), LocalTime.of(6, 0)))

        viewModel.onQuietHoursChanged(false)

        assertNull(viewModel.uiState.value.rules.quietStart)
        assertNull(viewModel.uiState.value.rules.quietEnd)
    }

    @Test
    fun `a window that starts and ends at the same time cannot be saved`() = runTest {
        val viewModel = viewModel()
        viewModel.onQuietHoursChanged(true)

        viewModel.onQuietEndChanged(LocalTime.of(21, 0))

        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onSaveClicked()
        coVerify(exactly = 0) { repository.setRules(any(), any()) }
    }

    @Test
    fun `saving sends the rules and leaves the screen`() = runTest {
        coEvery { repository.setRules("org", any()) } returns AdminResult.Success(Unit)
        val viewModel = viewModel()
        viewModel.onBlockChanged(true)

        viewModel.onSaveClicked()

        coVerify { repository.setRules("org", MessagingRules(blockStaffStudentDirectMessages = true)) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `a refused save is reported`() = runTest {
        coEvery { repository.setRules("org", any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = viewModel()

        viewModel.onSaveClicked()

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
