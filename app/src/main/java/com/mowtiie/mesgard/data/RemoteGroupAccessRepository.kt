package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.AccessedMessageDto
import com.mowtiie.mesgard.data.remote.GroupAccessRemoteSource
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.domain.model.AccessedMessage
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.GroupAccessRepository
import javax.inject.Inject

internal class RemoteGroupAccessRepository @Inject constructor(private val remote: GroupAccessRemoteSource) :
    GroupAccessRepository {
    override suspend fun open(groupId: String, reason: String): AdminResult<Unit> =
        attemptAdmin { remote.open(groupId, reason.trim()) }

    override suspend fun messages(
        groupId: String,
        before: AccessedMessage?,
        pageSize: Int,
    ): AdminResult<List<AccessedMessage>> = attemptAdmin {
        remote.messages(groupId, before?.cursorCreatedAt, before?.id, pageSize).map { it.toMessage() }
    }

    override suspend fun openings(groupId: String): AdminResult<List<Long>> =
        attemptAdmin { remote.openings(groupId).map { parseEpochMillis(it.openedAt) } }
}

private fun AccessedMessageDto.toMessage() = AccessedMessage(
    id = messageId,
    senderName = senderName,
    body = body,
    createdAtEpochMillis = parseEpochMillis(createdAt),
    cursorCreatedAt = createdAt,
    isDeleted = deletedAt != null,
    hasAttachments = hasAttachments,
)
