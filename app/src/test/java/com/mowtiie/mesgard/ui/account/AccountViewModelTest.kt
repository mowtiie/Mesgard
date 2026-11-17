package com.mowtiie.mesgard.ui.account

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.MfaError
import com.mowtiie.mesgard.domain.model.MfaResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.MfaRepository
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

class AccountViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val account = MutableStateFlow<Account?>(null)
    private val repository = mockk<AccountRepository> {
        every { observeAccount() } returns account
        coEvery { refresh() } returns RefreshResult.Success
    }

    private val isMfaEnabled = MutableStateFlow(false)
    private val mfaRepository = mockk<MfaRepository> {
        every { isEnabled } returns isMfaEnabled
    }

    private fun accountWith(role: OrgRole) =
        Account("Ana Reyes", "ana@school.test", listOf(AccountMembership("org", "School", role)))

    @Test
    fun `loads the account when it is created`() = runTest {
        AccountViewModel(repository, mfaRepository).uiState.test { cancelAndIgnoreRemainingEvents() }

        coVerify(exactly = 1) { repository.refresh() }
    }

    @Test
    fun `exposes the stored account`() = runTest {
        account.value = Account("Ana Reyes", "ana@school.test", emptyList())

        AccountViewModel(repository, mfaRepository).uiState.test {
            assertEquals("Ana Reyes", expectMostRecentItem().account?.displayName)
        }
    }

    @Test
    fun `has no account and is not refreshing once the load fails`() = runTest {
        coEvery { repository.refresh() } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        AccountViewModel(repository, mfaRepository).uiState.test {
            val state = expectMostRecentItem()
            assertNull(state.account)
            assertFalse(state.isRefreshing)
            assertEquals(AccountError.REFRESH_NETWORK_UNAVAILABLE, state.error)
        }
    }

    @Test
    fun `clears the error once it has been shown`() = runTest {
        coEvery { repository.refresh() } returns RefreshResult.Failure(RefreshError.UNKNOWN)
        val viewModel = AccountViewModel(repository, mfaRepository)

        viewModel.uiState.test {
            assertEquals(AccountError.REFRESH_UNKNOWN, expectMostRecentItem().error)
            viewModel.onErrorShown()
            assertNull(awaitItem().error)
        }
    }

    @Test
    fun `asks for confirmation before signing out and can be dismissed`() = runTest {
        val viewModel = AccountViewModel(repository, mfaRepository)

        viewModel.uiState.test {
            assertFalse(expectMostRecentItem().showSignOutConfirmation)
            viewModel.onSignOutClicked()
            assertTrue(awaitItem().showSignOutConfirmation)
            viewModel.onSignOutDismissed()
            assertFalse(awaitItem().showSignOutConfirmation)
        }
    }

    @Test
    fun `shows two-step verification as off until a factor is confirmed`() = runTest {
        account.value = accountWith(OrgRole.STUDENT)

        AccountViewModel(repository, mfaRepository).uiState.test {
            assertEquals(MfaRowState.OFF, expectMostRecentItem().mfa)
            isMfaEnabled.value = true
            assertEquals(MfaRowState.ON, awaitItem().mfa)
        }
    }

    @Test
    fun `requires two-step verification for administrators and staff`() = runTest {
        isMfaEnabled.value = true

        AccountViewModel(repository, mfaRepository).uiState.test {
            account.value = accountWith(OrgRole.STAFF)
            assertEquals(MfaRowState.REQUIRED, expectMostRecentItem().mfa)
            account.value = accountWith(OrgRole.ORG_ADMIN)
            assertEquals(MfaRowState.REQUIRED, expectMostRecentItem().mfa)
        }
    }

    @Test
    fun `turns two-step verification off after the confirmation`() = runTest {
        coEvery { mfaRepository.disable() } returns MfaResult.Success(Unit)
        val viewModel = AccountViewModel(repository, mfaRepository)

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onDisableMfaClicked()
            assertTrue(awaitItem().showDisableMfaConfirmation)
            viewModel.onDisableMfaConfirmed()
            val state = expectMostRecentItem()
            assertFalse(state.showDisableMfaConfirmation)
            assertFalse(state.isWorking)
            assertNull(state.error)
        }
        coVerify(exactly = 1) { mfaRepository.disable() }
    }

    @Test
    fun `keeps the confirmation and shows the error when turning it off fails`() = runTest {
        coEvery { mfaRepository.disable() } returns MfaResult.Failure(MfaError.NETWORK_UNAVAILABLE)
        val viewModel = AccountViewModel(repository, mfaRepository)

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onDisableMfaClicked()
            awaitItem()
            viewModel.onDisableMfaConfirmed()
            val state = expectMostRecentItem()
            assertTrue(state.showDisableMfaConfirmation)
            assertEquals(AccountError.MFA_NETWORK_UNAVAILABLE, state.error)
        }
    }
}
