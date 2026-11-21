package com.mowtiie.mesgard.domain.reports

import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FindReviewOrganizationTest {
    private val accountRepository = mockk<AccountRepository>()
    private val reviewRepository = mockk<ReportReviewRepository>()
    private val findReviewOrganization = FindReviewOrganization(accountRepository, reviewRepository)

    private fun account(vararg memberships: AccountMembership) {
        every { accountRepository.observeAccount() } returns flowOf(Account("Ana", null, memberships.toList()))
    }

    @Test
    fun `a student is never asked about`() = runTest {
        account(AccountMembership("o1", "School", OrgRole.MEMBER))

        assertNull(findReviewOrganization())
        coVerify(exactly = 0) { reviewRepository.canReview(any()) }
    }

    @Test
    fun `the first organization where the server says the person reviews is the one`() = runTest {
        account(
            AccountMembership("o1", "One", OrgRole.STAFF),
            AccountMembership("o2", "Two", OrgRole.ORG_ADMIN),
        )
        coEvery { reviewRepository.canReview("o1") } returns AdminResult.Success(false)
        coEvery { reviewRepository.canReview("o2") } returns AdminResult.Success(true)

        assertEquals("o2", findReviewOrganization())
    }

    @Test
    fun `it waits for the account that is stored a moment after signing in`() = runTest {
        val stored = MutableStateFlow<Account?>(null)
        every { accountRepository.observeAccount() } returns stored
        coEvery { reviewRepository.canReview("o1") } returns AdminResult.Success(true)
        val found = async { findReviewOrganization() }

        stored.value = Account("Ana", null, listOf(AccountMembership("o1", "One", OrgRole.ORG_ADMIN)))

        assertEquals("o1", found.await())
    }

    @Test
    fun `an account that never arrives means no reports`() = runTest {
        every { accountRepository.observeAccount() } returns MutableStateFlow(null)

        assertNull(findReviewOrganization())
    }

    @Test
    fun `a failed answer means the person is not shown reports`() = runTest {
        account(AccountMembership("o1", "One", OrgRole.STAFF))
        coEvery { reviewRepository.canReview("o1") } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)

        assertNull(findReviewOrganization())
    }
}
