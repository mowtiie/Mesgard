package com.mowtiie.mesgard.ui.legalhold

import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.HoldTarget
import com.mowtiie.mesgard.domain.model.LegalHold
import com.mowtiie.mesgard.domain.model.LegalHoldScope
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.LegalHoldRepository
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

class LegalHoldViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<LegalHoldRepository>()
    private val accountRepository = mockk<AccountRepository>()

    private fun admin(): ObserveAdminOrganization {
        every { accountRepository.observeAccount() } returns
            flowOf(Account("Ana", null, listOf(AccountMembership("org", "School", OrgRole.ORG_ADMIN))))
        return ObserveAdminOrganization(accountRepository)
    }

    private fun hold(id: String) =
        LegalHold(id, LegalHoldScope.PERSON, "Student Two", null, "A court order", "Admin", 0L)

    @Test
    fun `placing a hold goes to the organization the person administers`() = runTest {
        coEvery { repository.place("org", any(), any()) } returns AdminResult.Success(Unit)
        val viewModel = PlaceHoldViewModel(admin(), repository)

        viewModel.onPlaceClicked(HoldTarget.Group("g1", "Section A"), "An inquiry covers the class")

        coVerify { repository.place("org", HoldTarget.Group("g1", "Section A"), "An inquiry covers the class") }
        assertTrue(viewModel.uiState.value.isPlaced)
    }

    @Test
    fun `a refused hold is reported and nothing is placed`() = runTest {
        coEvery { repository.place("org", any(), any()) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val viewModel = PlaceHoldViewModel(admin(), repository)

        viewModel.onPlaceClicked(HoldTarget.Organization, "An inquiry covers the school")

        assertEquals(AdminError.NOT_ALLOWED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isPlaced)
    }

    @Test
    fun `the next dialog starts clean once the hold was handled`() = runTest {
        coEvery { repository.place("org", any(), any()) } returns AdminResult.Success(Unit)
        val viewModel = PlaceHoldViewModel(admin(), repository)
        viewModel.onPlaceClicked(HoldTarget.Organization, "An inquiry covers the school")

        viewModel.onPlacedHandled()

        assertFalse(viewModel.uiState.value.isPlaced)
    }

    @Test
    fun `the active holds are listed`() = runTest {
        coEvery { repository.activeHolds("org") } returns AdminResult.Success(listOf(hold("h1")))

        val state = LegalHoldsViewModel(admin(), repository).uiState.value

        assertEquals(listOf("h1"), state.holds.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun `no holds shows the empty message`() = runTest {
        coEvery { repository.activeHolds("org") } returns AdminResult.Success(emptyList())

        assertTrue(LegalHoldsViewModel(admin(), repository).uiState.value.showEmptyMessage)
    }

    @Test
    fun `releasing a hold sends the note and lists again`() = runTest {
        coEvery { repository.activeHolds("org") } returnsMany listOf(
            AdminResult.Success(listOf(hold("h1"))),
            AdminResult.Success(emptyList()),
        )
        coEvery { repository.release("h1", any()) } returns AdminResult.Success(Unit)
        val viewModel = LegalHoldsViewModel(admin(), repository)
        viewModel.onReleaseRequested(hold("h1"))

        viewModel.onReleaseConfirmed("The order was withdrawn")

        coVerify { repository.release("h1", "The order was withdrawn") }
        assertTrue(viewModel.uiState.value.holds.isEmpty())
        assertNull(viewModel.uiState.value.holdToRelease)
    }

    @Test
    fun `releasing nothing sends nothing`() = runTest {
        coEvery { repository.activeHolds("org") } returns AdminResult.Success(listOf(hold("h1")))
        val viewModel = LegalHoldsViewModel(admin(), repository)

        viewModel.onReleaseConfirmed("The order was withdrawn")

        coVerify(exactly = 0) { repository.release(any(), any()) }
    }
}
