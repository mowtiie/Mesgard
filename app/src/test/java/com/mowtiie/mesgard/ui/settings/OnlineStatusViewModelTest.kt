package com.mowtiie.mesgard.ui.settings

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.OnlineStatusRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class OnlineStatusViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val choice = MutableStateFlow<Boolean?>(null)
    private val onlineStatus = mockk<OnlineStatusRepository> {
        every { this@mockk.choice } returns this@OnlineStatusViewModelTest.choice
        coEvery { refresh() } returns RefreshResult.Success
        coEvery { setChoice(any()) } returns RefreshResult.Success
    }
    private val accountRepository = mockk<AccountRepository>()

    private fun viewModel(vararg roles: OrgRole): OnlineStatusViewModel {
        val memberships = roles.mapIndexed { index, role -> AccountMembership("org$index", "School $index", role) }
        every { accountRepository.observeAccount() } returns flowOf(Account("Ana", null, memberships))
        return OnlineStatusViewModel(onlineStatus, accountRepository)
    }

    @Test
    fun `a student is hidden until they choose`() = runTest {
        viewModel(OrgRole.MEMBER).isShown.test { assertEquals(false, expectMostRecentItem()) }
    }

    @Test
    fun `staff and administrators are shown until they choose`() = runTest {
        viewModel(OrgRole.STAFF).isShown.test { assertEquals(true, expectMostRecentItem()) }
        viewModel(OrgRole.ORG_ADMIN).isShown.test { assertEquals(true, expectMostRecentItem()) }
    }

    @Test
    fun `a person who is a student in one place and staff in another is shown`() = runTest {
        viewModel(OrgRole.MEMBER, OrgRole.STAFF).isShown.test { assertEquals(true, expectMostRecentItem()) }
    }

    @Test
    fun `an own choice beats the default`() = runTest {
        choice.value = true
        viewModel(OrgRole.MEMBER).isShown.test { assertEquals(true, expectMostRecentItem()) }
        choice.value = false
        viewModel(OrgRole.STAFF).isShown.test { assertEquals(false, expectMostRecentItem()) }
    }

    @Test
    fun `nothing is decided until the account is known`() = runTest {
        every { accountRepository.observeAccount() } returns flowOf<Account?>(null)

        OnlineStatusViewModel(onlineStatus, accountRepository).isShown.test { assertNull(expectMostRecentItem()) }
    }

    @Test
    fun `the choice is read from the server and changing the switch saves it`() = runTest {
        val viewModel = viewModel(OrgRole.MEMBER)

        viewModel.onChanged(true)

        coVerify { onlineStatus.refresh() }
        coVerify { onlineStatus.setChoice(true) }
    }
}
