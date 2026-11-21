package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.PrivacyNoticeDto
import com.mowtiie.mesgard.data.remote.PrivacyNoticeRemoteSource
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.PrivacyNotice
import com.mowtiie.mesgard.domain.repository.PrivacyNoticeRepository
import javax.inject.Inject

internal class RemotePrivacyNoticeRepository @Inject constructor(private val remote: PrivacyNoticeRemoteSource) :
    PrivacyNoticeRepository {
    override suspend fun notices(): AdminResult<List<PrivacyNotice>> =
        attemptAdmin { remote.notices().map { it.toNotice() } }

    override suspend fun acknowledge(orgId: String): AdminResult<Unit> =
        attemptAdmin<Unit> { remote.acknowledge(orgId) }

    override suspend fun setLinks(orgId: String, noticeUrl: String, childNoticeUrl: String): AdminResult<Unit> =
        attemptAdmin<Unit> {
            remote.setLinks(orgId, noticeUrl.trim().ifEmpty { null }, childNoticeUrl.trim().ifEmpty { null })
        }
}

private fun PrivacyNoticeDto.toNotice() = PrivacyNotice(
    orgId = orgId,
    orgName = orgName,
    noticeUrl = noticeUrl,
    childNoticeUrl = childNoticeUrl,
    isAcknowledged = isAcknowledged,
)
