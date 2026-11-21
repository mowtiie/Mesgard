package com.mowtiie.mesgard.domain.reports

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val ACCOUNT_WAIT_MS = 10_000L

/**
 * The organization whose reports the signed in person reviews, or null for everyone else. Only administrators and
 * staff can be reviewers, and the server says whether they are, so a person who reviews in several organizations
 * reviews the first one. Right after signing in the account is not stored yet, so it waits a little for it.
 */
internal class FindReviewOrganization @Inject constructor(
    private val accountRepository: AccountRepository,
    private val reviewRepository: ReportReviewRepository,
) {
    suspend operator fun invoke(): String? {
        val account = withTimeoutOrNull(ACCOUNT_WAIT_MS) { accountRepository.observeAccount().filterNotNull().first() }
        return account?.memberships.orEmpty()
            .filter { it.role == OrgRole.ORG_ADMIN || it.role == OrgRole.STAFF }
            .firstOrNull { reviewRepository.canReview(it.organizationId) == AdminResult.Success(true) }
            ?.organizationId
    }
}
