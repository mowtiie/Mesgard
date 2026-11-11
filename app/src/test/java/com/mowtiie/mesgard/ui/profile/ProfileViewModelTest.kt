package com.mowtiie.mesgard.ui.profile

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
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

class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val account = MutableStateFlow<Account?>(null)
    private val repository = mockk<AccountRepository> {
        every { observeAccount() } returns account
        coEvery { refresh() } returns RefreshResult.Success
    }

    @Test
    fun `loads the account when it is created`() = runTest {
        ProfileViewModel(repository).uiState.test { cancelAndIgnoreRemainingEvents() }

        coVerify(exactly = 1) { repository.refresh() }
    }

    @Test
    fun `exposes the stored account`() = runTest {
        account.value = Account("Ana Reyes", "ana@school.test", emptyList())

        ProfileViewModel(repository).uiState.test {
            assertEquals("Ana Reyes", expectMostRecentItem().account?.displayName)
        }
    }

    @Test
    fun `has no account and is not refreshing once the load fails`() = runTest {
        coEvery { repository.refresh() } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        ProfileViewModel(repository).uiState.test {
            val state = expectMostRecentItem()
            assertNull(state.account)
            assertFalse(state.isRefreshing)
            assertEquals(ProfileError.REFRESH_NETWORK_UNAVAILABLE, state.error)
        }
    }

    @Test
    fun `clears the error once it has been shown`() = runTest {
        coEvery { repository.refresh() } returns RefreshResult.Failure(RefreshError.UNKNOWN)
        val viewModel = ProfileViewModel(repository)

        viewModel.uiState.test {
            assertEquals(ProfileError.REFRESH_UNKNOWN, expectMostRecentItem().error)
            viewModel.onErrorShown()
            assertNull(awaitItem().error)
        }
    }

    @Test
    fun `asks for confirmation before signing out and can be dismissed`() = runTest {
        val viewModel = ProfileViewModel(repository)

        viewModel.uiState.test {
            assertFalse(expectMostRecentItem().showSignOutConfirmation)
            viewModel.onSignOutClicked()
            assertTrue(awaitItem().showSignOutConfirmation)
            viewModel.onSignOutDismissed()
            assertFalse(awaitItem().showSignOutConfirmation)
        }
    }
}
