package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.GroupCreationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * The organization where the signed in person can create groups: the first they administer, otherwise the first whose
 * policy lets them. It is null when there is none, and when the server cannot be asked.
 */
internal class FindGroupCreationOrganization @Inject constructor(
    private val accountRepository: AccountRepository,
    private val groupCreationRepository: GroupCreationRepository,
) {
    suspend operator fun invoke(): AccountMembership? {
        val memberships = accountRepository.observeAccount().first()?.memberships.orEmpty()
        return memberships.firstOrNull { it.role == OrgRole.ORG_ADMIN }
            ?: memberships.firstOrNull { canCreateIn(it.organizationId) }
    }

    private suspend fun canCreateIn(organizationId: String): Boolean =
        (groupCreationRepository.rules(organizationId) as? AdminResult.Success)?.value?.canCreate == true
}
