package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.GroupCreationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/** The account is stored a moment after sign in, first without its organizations, so the check waits for them. */
private const val ACCOUNT_WAIT_MS = 10_000L

/**
 * The organization where the signed in person can create groups: the first they administer, otherwise the first whose
 * policy lets them. It is null when there is none, and when the server cannot be asked.
 */
internal class FindGroupCreationOrganization @Inject constructor(
    private val accountRepository: AccountRepository,
    private val groupCreationRepository: GroupCreationRepository,
) {
    suspend operator fun invoke(): AccountMembership? {
        val memberships = withTimeoutOrNull(ACCOUNT_WAIT_MS) {
            accountRepository.observeAccount().map { it?.memberships.orEmpty() }.first { it.isNotEmpty() }
        }.orEmpty()
        return memberships.firstOrNull { it.role == OrgRole.ORG_ADMIN }
            ?: memberships.firstOrNull { canCreateIn(it.organizationId) }
    }

    private suspend fun canCreateIn(organizationId: String): Boolean =
        (groupCreationRepository.rules(organizationId) as? AdminResult.Success)?.value?.canCreate == true
}
