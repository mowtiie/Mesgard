package com.mowtiie.mesgard.ui.admin.audit

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.admin.ObserveAdminOrganization
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.AuditCursor
import com.mowtiie.mesgard.domain.model.AuditEntry
import com.mowtiie.mesgard.domain.model.AuditPage
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AuditLogRepository
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ContactRepository
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

class AuditLogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val account = Account("Admin Cruz", null, listOf(AccountMembership(ORG, "Rizal", OrgRole.ORG_ADMIN)))
    private val accountRepository = mockk<AccountRepository> {
        every { observeAccount() } returns
            MutableStateFlow(account)
    }
    private val contactRepository = mockk<ContactRepository> {
        every { observeContacts() } returns MutableStateFlow(listOf(Contact(ORG, "u1", "Ana Reyes", OrgRole.MEMBER)))
    }
    private val authRepository = mockk<AuthRepository> { every { currentUserId } returns "me" }
    private val auditLogRepository = mockk<AuditLogRepository>()

    private fun entry(number: Int) = AuditEntry("id-$number", "me", "member.added", "u1", emptyMap(), number.toLong())

    private fun viewModel() = AuditLogViewModel(
        ObserveAdminOrganization(accountRepository),
        auditLogRepository,
        contactRepository,
        accountRepository,
        authRepository,
    )

    @Test
    fun `shows the newest entries with the names of the roster and of the admin`() = runTest {
        coEvery { auditLogRepository.fetchPage(ORG, null) } returns
            AdminResult.Success(AuditPage(listOf(entry(2), entry(1)), null))

        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("id-2", "id-1"), state.entries.map { it.id })
            assertEquals(mapOf("u1" to "Ana Reyes", "me" to "Admin Cruz"), state.names)
            assertFalse(state.hasMore)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `loads the next page after the cursor and appends it`() = runTest {
        val cursor = AuditCursor("2026-11-14T16:56:01+00:00", "id-1")
        coEvery { auditLogRepository.fetchPage(ORG, null) } returns
            AdminResult.Success(AuditPage(listOf(entry(2), entry(1)), cursor))
        coEvery { auditLogRepository.fetchPage(ORG, cursor) } returns
            AdminResult.Success(AuditPage(listOf(entry(0)), null))
        val viewModel = viewModel()
        viewModel.uiState.test { assertTrue(expectMostRecentItem().hasMore) }

        viewModel.onLoadMore()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("id-2", "id-1", "id-0"), state.entries.map { it.id })
            assertFalse(state.hasMore)
        }
    }

    @Test
    fun `refreshing replaces what was shown`() = runTest {
        coEvery { auditLogRepository.fetchPage(ORG, null) } returnsMany listOf(
            AdminResult.Success(AuditPage(listOf(entry(1)), null)),
            AdminResult.Success(AuditPage(listOf(entry(2)), null)),
        )
        val viewModel = viewModel()

        viewModel.onRefresh()

        viewModel.uiState.test { assertEquals(listOf("id-2"), expectMostRecentItem().entries.map { it.id }) }
    }

    @Test
    fun `does nothing when there is no next page`() = runTest {
        coEvery { auditLogRepository.fetchPage(ORG, null) } returns
            AdminResult.Success(AuditPage(listOf(entry(1)), null))
        val viewModel = viewModel()

        viewModel.onLoadMore()

        coVerify(exactly = 1) { auditLogRepository.fetchPage(any(), any()) }
    }

    @Test
    fun `says when there is no activity and reports a failure`() = runTest {
        coEvery { auditLogRepository.fetchPage(ORG, null) } returns AdminResult.Success(AuditPage(emptyList(), null))
        viewModel().uiState.test { assertTrue(expectMostRecentItem().showEmptyMessage) }

        coEvery { auditLogRepository.fetchPage(ORG, null) } returns AdminResult.Failure(AdminError.NOT_ALLOWED)
        val failing = viewModel()
        failing.uiState.test { assertEquals(AdminError.NOT_ALLOWED, expectMostRecentItem().error) }
        failing.onErrorShown()
        failing.uiState.test { assertNull(expectMostRecentItem().error) }
    }

    private companion object {
        const val ORG = "org"
    }
}
