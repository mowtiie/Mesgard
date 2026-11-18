package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.FormerMemberCursor
import com.mowtiie.mesgard.domain.model.FormerMembersPage

internal const val FORMER_MEMBERS_PAGE_SIZE = 50

internal interface FormerMembersRepository {
    /** The people the organization removed, newest first, or the page after [before]. Administrators only. */
    suspend fun fetchPage(orgId: String, before: FormerMemberCursor?): AdminResult<FormerMembersPage>
}
