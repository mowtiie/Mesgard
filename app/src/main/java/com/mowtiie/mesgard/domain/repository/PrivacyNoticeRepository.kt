package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.PrivacyNotice

/** The privacy notices of the organizations of the signed in person. They need a connection and are never kept. */
internal interface PrivacyNoticeRepository {
    suspend fun notices(): AdminResult<List<PrivacyNotice>>

    suspend fun acknowledge(orgId: String): AdminResult<Unit>

    /** Only an organization admin can set the links. A blank link removes it. */
    suspend fun setLinks(orgId: String, noticeUrl: String, childNoticeUrl: String): AdminResult<Unit>
}
