package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.LegalHoldDto
import com.mowtiie.mesgard.data.remote.LegalHoldRemoteSource
import com.mowtiie.mesgard.data.remote.PlaceHoldRequest
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.HoldTarget
import com.mowtiie.mesgard.domain.model.LegalHold
import com.mowtiie.mesgard.domain.model.LegalHoldScope
import com.mowtiie.mesgard.domain.repository.LegalHoldRepository
import javax.inject.Inject

internal class RemoteLegalHoldRepository @Inject constructor(private val remote: LegalHoldRemoteSource) :
    LegalHoldRepository {
    override suspend fun activeHolds(orgId: String): AdminResult<List<LegalHold>> =
        attemptAdmin { remote.activeHolds(orgId).map { it.toHold() } }

    override suspend fun place(orgId: String, target: HoldTarget, reason: String): AdminResult<Unit> =
        attemptAdmin<Unit> {
            remote.place(
                PlaceHoldRequest(
                    orgId = orgId,
                    reason = reason.trim(),
                    userId = (target as? HoldTarget.Person)?.userId,
                    groupId = (target as? HoldTarget.Group)?.groupId,
                ),
            )
        }

    override suspend fun release(holdId: String, note: String): AdminResult<Unit> =
        attemptAdmin<Unit> { remote.release(holdId, note.trim()) }
}

private fun LegalHoldDto.toHold() = LegalHold(
    id = holdId,
    scope = when (scope) {
        "person" -> LegalHoldScope.PERSON
        "group" -> LegalHoldScope.GROUP
        else -> LegalHoldScope.ORGANIZATION
    },
    personName = userName,
    groupName = groupName,
    reason = reason,
    placedByName = placedByName,
    placedAtEpochMillis = parseEpochMillis(placedAt),
)
