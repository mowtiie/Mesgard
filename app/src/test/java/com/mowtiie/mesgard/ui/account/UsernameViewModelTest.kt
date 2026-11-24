package com.mowtiie.mesgard.ui.account

import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.UsernamePolicy
import com.mowtiie.mesgard.domain.model.UsernameRules
import com.mowtiie.mesgard.domain.repository.UsernameRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UsernameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<UsernameRepository>()
    private val membership = AccountMembership("org1", "Rizal National", OrgRole.MEMBER)

    private fun rules(username: String = "ana.cruz", allows: Boolean = true, nextChange: Long? = null) =
        UsernameRules("org1", UsernamePolicy.EVERYONE, username, allows, nextChange)

    @Test
    fun `a row appears for each organization the server answers for`() = runTest {
        coEvery { repository.rules("org1") } returns AdminResult.Success(rules())
        coEvery { repository.rules("org2") } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        val viewModel = UsernameViewModel(repository)

        viewModel.onMembershipsChanged(listOf(membership, AccountMembership("org2", "Other", OrgRole.STAFF)))

        assertEquals(listOf("ana.cruz"), viewModel.uiState.value.rows.map { it.rules.username })
    }

    @Test
    fun `a username can be changed only when the policy allows it and the wait is over`() = runTest {
        coEvery { repository.rules("org1") } returnsMany listOf(
            AdminResult.Success(rules()),
            AdminResult.Success(rules(allows = false)),
            AdminResult.Success(rules(nextChange = 1L)),
        )
        val viewModel = UsernameViewModel(repository)

        viewModel.onMembershipsChanged(listOf(membership))
        assertTrue(viewModel.uiState.value.rows.single().canChange)
        viewModel.onMembershipsChanged(listOf(membership.copy(organizationName = "Renamed")))
        assertFalse(viewModel.uiState.value.rows.single().canChange)
        viewModel.onMembershipsChanged(listOf(membership.copy(organizationName = "Renamed again")))
        assertFalse(viewModel.uiState.value.rows.single().canChange)
    }

    @Test
    fun `saving sends the text and reads the username again`() = runTest {
        coEvery { repository.rules("org1") } returnsMany
            listOf(AdminResult.Success(rules()), AdminResult.Success(rules(username = "ana.c")))
        coEvery { repository.change("org1", "ana.c") } returns AdminResult.Success(Unit)
        val viewModel = UsernameViewModel(repository)
        viewModel.onMembershipsChanged(listOf(membership))

        viewModel.onEditRequested(viewModel.uiState.value.rows.single())
        viewModel.onTextChanged("ana.c")
        viewModel.onSaveClicked()

        coVerify { repository.change("org1", "ana.c") }
        assertNull(viewModel.uiState.value.edit)
        assertEquals("ana.c", viewModel.uiState.value.rows.single().rules.username)
    }

    @Test
    fun `a refused username stays in the sheet with the reason`() = runTest {
        coEvery { repository.rules("org1") } returns AdminResult.Success(rules())
        coEvery { repository.change("org1", "admin") } returns AdminResult.Failure(AdminError.USERNAME_RESERVED)
        val viewModel = UsernameViewModel(repository)
        viewModel.onMembershipsChanged(listOf(membership))

        viewModel.onEditRequested(viewModel.uiState.value.rows.single())
        viewModel.onTextChanged("admin")
        viewModel.onSaveClicked()

        val edit = viewModel.uiState.value.edit
        assertEquals(AdminError.USERNAME_RESERVED, edit?.error)
        assertFalse(edit?.isSaving ?: true)
        viewModel.onTextChanged("admin2")
        assertNull(viewModel.uiState.value.edit?.error)
    }
}
