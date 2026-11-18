package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.FormerMemberDto
import com.mowtiie.mesgard.data.remote.FormerMembersRemoteSource
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.FormerMember
import com.mowtiie.mesgard.domain.model.FormerMemberCursor
import com.mowtiie.mesgard.domain.model.FormerMembersPage
import com.mowtiie.mesgard.domain.repository.FORMER_MEMBERS_PAGE_SIZE
import com.mowtiie.mesgard.domain.repository.FormerMembersRepository
import javax.inject.Inject

internal class RemoteFormerMembersRepository @Inject constructor(private val remote: FormerMembersRemoteSource) :
    FormerMembersRepository {
    override suspend fun fetchPage(orgId: String, before: FormerMemberCursor?): AdminResult<FormerMembersPage> =
        attemptAdmin {
            val rows = remote.fetchPage(orgId, before, FORMER_MEMBERS_PAGE_SIZE)
            val last = rows.lastOrNull()
            FormerMembersPage(
                members = rows.map { it.toFormerMember() },
                next = if (rows.size == FORMER_MEMBERS_PAGE_SIZE && last != null) {
                    FormerMemberCursor(last.removedAt, last.userId)
                } else {
                    null
                },
            )
        }
}

private fun FormerMemberDto.toFormerMember() = FormerMember(
    userId = userId,
    displayName = displayName,
    removedAtEpochMillis = parseEpochMillis(removedAt),
    isErased = isErased,
    canErase = canErase,
)
