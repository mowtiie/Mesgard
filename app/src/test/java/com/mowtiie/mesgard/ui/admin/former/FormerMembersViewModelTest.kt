package com.mowtiie.mesgard.ui.admin.former

import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ErasureMode
import com.mowtiie.mesgard.domain.model.FormerMember
import com.mowtiie.mesgard.domain.model.FormerMemberCursor
import com.mowtiie.mesgard.domain.model.FormerMembersPage
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AdminRepository
import com.mowtiie.mesgard.domain.repository.FormerMembersRepository
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

private const val ORG = "org-1"

class FormerMembersViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns
            MutableStateFlow(Account("Admin", null, listOf(AccountMembership(ORG, "Example", OrgRole.ORG_ADMIN))))
    }
    private val formerMembersRepository = mockk<FormerMembersRepository>()
    private val adminRepository = mockk<AdminRepository>()

    private fun person(number: Int, canErase: Boolean = true, erased: Boolean = false) =
        FormerMember("u$number", "Person $number", number.toLong(), isErased = erased, canErase = canErase)

    private fun viewModel(mode: ErasureMode = ErasureMode.KEEP_MESSAGES): FormerMembersViewModel {
        coEvery { adminRepository.erasureMode() } returns AdminResult.Success(mode)
        return FormerMembersViewModel(
            ObserveAdminOrganization(accountRepository),
            formerMembersRepository,
            adminRepository,
        )
    }

    @Test
    fun `lists the people the organization removed`() = runTest {
        coEvery { formerMembersRepository.fetchPage(ORG, null) } returns
            AdminResult.Success(FormerMembersPage(listOf(person(2), person(1)), null))

        val state = viewModel().uiState.value

        assertEquals(listOf("u2", "u1"), state.members.map { it.userId })
        assertFalse(state.isLoading)
        assertFalse(state.hasMore)
    }

    @Test
    fun `loads the next page after the cursor and appends it`() = runTest {
        val cursor = FormerMemberCursor("2026-11-14T16:56:01+00:00", "u1")
        coEvery { formerMembersRepository.fetchPage(ORG, null) } returns
            AdminResult.Success(FormerMembersPage(listOf(person(2), person(1)), cursor))
        coEvery { formerMembersRepository.fetchPage(ORG, cursor) } returns
            AdminResult.Success(FormerMembersPage(listOf(person(0)), null))
        val viewModel = viewModel()

        viewModel.onLoadMore()

        assertEquals(listOf("u2", "u1", "u0"), viewModel.uiState.value.members.map { it.userId })
        assertFalse(viewModel.uiState.value.hasMore)
    }

    @Test
    fun `offers Erase only to someone who can still be erased on a server that allows it`() = runTest {
        coEvery { formerMembersRepository.fetchPage(ORG, null) } returns
            AdminResult.Success(FormerMembersPage(emptyList(), null))

        assertTrue(viewModel(ErasureMode.BLANK_MESSAGES).uiState.value.canErase(person(1)))
        assertFalse(viewModel(ErasureMode.KEEP_MESSAGES).uiState.value.canErase(person(1, canErase = false)))
        assertFalse(viewModel(ErasureMode.OFF).uiState.value.canErase(person(1)))
    }

    @Test
    fun `erasing asks first, then erases and reloads the list`() = runTest {
        coEvery { formerMembersRepository.fetchPage(ORG, null) } returnsMany listOf(
            AdminResult.Success(FormerMembersPage(listOf(person(1)), null)),
            AdminResult.Success(FormerMembersPage(listOf(person(1, canErase = false, erased = true)), null)),
        )
        coEvery { adminRepository.eraseMember(ORG, "u1") } returns AdminResult.Success(Unit)
        val viewModel = viewModel()

        viewModel.onEraseClicked(person(1))
        assertEquals("u1", viewModel.uiState.value.confirming?.userId)
        viewModel.onEraseConfirmed()

        coVerify(exactly = 1) { adminRepository.eraseMember(ORG, "u1") }
        assertNull(viewModel.uiState.value.confirming)
        assertTrue(viewModel.uiState.value.members.single().isErased)
    }

    @Test
    fun `cancelling the question erases nothing`() = runTest {
        coEvery { formerMembersRepository.fetchPage(ORG, null) } returns
            AdminResult.Success(FormerMembersPage(listOf(person(1)), null))
        val viewModel = viewModel()

        viewModel.onEraseClicked(person(1))
        viewModel.onEraseDismissed()
        viewModel.onEraseConfirmed()

        assertNull(viewModel.uiState.value.confirming)
        coVerify(exactly = 0) { adminRepository.eraseMember(any(), any()) }
    }

    @Test
    fun `a refused erase shows the reason and keeps the person listed`() = runTest {
        coEvery { formerMembersRepository.fetchPage(ORG, null) } returns
            AdminResult.Success(FormerMembersPage(listOf(person(1)), null))
        coEvery { adminRepository.eraseMember(ORG, "u1") } returns AdminResult.Failure(AdminError.NOT_ERASABLE)
        val viewModel = viewModel()

        viewModel.onEraseClicked(person(1))
        viewModel.onEraseConfirmed()

        assertEquals(AdminError.NOT_ERASABLE, viewModel.uiState.value.error)
        assertEquals(listOf("u1"), viewModel.uiState.value.members.map { it.userId })
        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `a failed load shows the reason`() = runTest {
        coEvery { formerMembersRepository.fetchPage(ORG, null) } returns
            AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)

        val state = viewModel().uiState.value

        assertEquals(AdminError.NETWORK_UNAVAILABLE, state.error)
        assertFalse(state.isLoading)
    }
}
