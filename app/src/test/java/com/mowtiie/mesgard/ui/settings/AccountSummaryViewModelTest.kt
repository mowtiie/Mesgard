package com.mowtiie.mesgard.ui.settings

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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AccountSummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val account = MutableStateFlow<Account?>(null)
    private val repository = mockk<AccountRepository> {
        every { observeAccount() } returns account
        coEvery { refresh() } returns RefreshResult.Success
    }

    @Test
    fun `refreshes the account once when it is created`() = runTest {
        AccountSummaryViewModel(repository).account.test { cancelAndIgnoreRemainingEvents() }

        coVerify(exactly = 1) { repository.refresh() }
    }

    @Test
    fun `shows no account until one is stored`() = runTest {
        AccountSummaryViewModel(repository).account.test { assertNull(expectMostRecentItem()) }
    }

    @Test
    fun `shows the stored account`() = runTest {
        account.value = Account("Ana Reyes", "ana@school.test", emptyList())

        AccountSummaryViewModel(repository).account.test {
            assertEquals("Ana Reyes", expectMostRecentItem()?.displayName)
        }
    }

    @Test
    fun `keeps the stored account when the refresh fails`() = runTest {
        account.value = Account("Ana Reyes", "ana@school.test", emptyList())
        coEvery { repository.refresh() } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        AccountSummaryViewModel(repository).account.test {
            assertEquals("Ana Reyes", expectMostRecentItem()?.displayName)
        }
    }
}
