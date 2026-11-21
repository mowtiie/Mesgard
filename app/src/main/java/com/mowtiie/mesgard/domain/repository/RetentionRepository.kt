package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult

/** How long an organization keeps messages. It needs a connection and is never kept on the phone. */
internal interface RetentionRepository {
    /** The limit in days, or null when messages are kept without a limit. */
    suspend fun retentionDays(orgId: String): AdminResult<Int?>

    /** Only an organization admin can set it. Null keeps messages without a limit. */
    suspend fun setRetentionDays(orgId: String, days: Int?): AdminResult<Unit>
}
