package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.AuditLogRemoteSource
import com.mowtiie.mesgard.data.remote.AuditRowDto
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.AuditCursor
import com.mowtiie.mesgard.domain.model.AuditEntry
import com.mowtiie.mesgard.domain.model.AuditPage
import com.mowtiie.mesgard.domain.repository.AUDIT_PAGE_SIZE
import com.mowtiie.mesgard.domain.repository.AuditLogRepository
import javax.inject.Inject
import kotlinx.serialization.json.JsonPrimitive

internal class RemoteAuditLogRepository @Inject constructor(private val remote: AuditLogRemoteSource) :
    AuditLogRepository {
    override suspend fun fetchPage(orgId: String, before: AuditCursor?): AdminResult<AuditPage> = attemptAdmin {
        val rows = remote.fetchPage(orgId, before, AUDIT_PAGE_SIZE)
        val last = rows.lastOrNull()
        AuditPage(
            entries = rows.map { it.toEntry() },
            next = if (rows.size == AUDIT_PAGE_SIZE && last != null) AuditCursor(last.createdAt, last.id) else null,
        )
    }
}

private fun AuditRowDto.toEntry() = AuditEntry(
    id = id,
    actorId = actorId,
    action = action,
    targetId = targetId,
    details = metadata.mapValues { (it.value as? JsonPrimitive)?.content.orEmpty() },
    createdAtEpochMillis = parseEpochMillis(createdAt),
)
