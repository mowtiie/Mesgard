package com.mowtiie.mesgard.domain.admin

import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The organization where the signed in user can post announcements: they are an admin or staff there. */
internal class ObservePostingOrganization @Inject constructor(accountRepository: AccountRepository) {
    val organization: Flow<AccountMembership?> = accountRepository.observeAccount().map { account ->
        account?.memberships?.firstOrNull { it.role == OrgRole.ORG_ADMIN || it.role == OrgRole.STAFF }
    }
}
