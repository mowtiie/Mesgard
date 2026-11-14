package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The organization the signed in user administers, or null for everyone else. A person who is an admin of several
 * organizations administers the first one.
 */
internal class ObserveAdminOrganization @Inject constructor(accountRepository: AccountRepository) {
    val organization: Flow<AccountMembership?> = accountRepository.observeAccount().map { account ->
        account?.memberships?.firstOrNull { it.role == OrgRole.ORG_ADMIN }
    }
}
