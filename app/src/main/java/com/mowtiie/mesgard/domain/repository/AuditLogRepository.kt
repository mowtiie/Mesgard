package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.AuditCursor
import com.mowtiie.mesgard.domain.model.AuditPage

internal const val AUDIT_PAGE_SIZE = 50

internal interface AuditLogRepository {
    /** The newest entries of the organization, or the page after [before]. Only organization admins can read them. */
    suspend fun fetchPage(orgId: String, before: AuditCursor?): AdminResult<AuditPage>
}
