package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.RetentionRemoteSource
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.RetentionRepository
import javax.inject.Inject

internal class RemoteRetentionRepository @Inject constructor(private val remote: RetentionRemoteSource) :
    RetentionRepository {
    override suspend fun retentionDays(orgId: String): AdminResult<Int?> = attemptAdmin { remote.retentionDays(orgId) }

    override suspend fun setRetentionDays(orgId: String, days: Int?): AdminResult<Unit> =
        attemptAdmin<Unit> { remote.setRetentionDays(orgId, days) }
}
