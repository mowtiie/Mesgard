package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ReportCursor
import com.mowtiie.mesgard.domain.model.ReportDecision
import com.mowtiie.mesgard.domain.model.ReportDetail
import com.mowtiie.mesgard.domain.model.ReportPage
import com.mowtiie.mesgard.domain.model.ReportReviewer
import com.mowtiie.mesgard.domain.model.ReportStatus

internal const val REPORT_PAGE_SIZE = 30

/** Reading and handling reports. It only works with a connection, and every answer comes from the server. */
internal interface ReportReviewRepository {
    suspend fun canReview(orgId: String): AdminResult<Boolean>

    suspend fun list(orgId: String, status: ReportStatus, before: ReportCursor?): AdminResult<ReportPage>

    /** Opening a case is recorded in its history. */
    suspend fun detail(reportId: String): AdminResult<ReportDetail>

    suspend fun decide(reportId: String, decision: ReportDecision): AdminResult<Unit>

    /** Only an organization admin can ask, with a reason that is recorded. Returns the name of the reporter. */
    suspend fun revealReporter(reportId: String, reason: String): AdminResult<String>

    suspend fun reviewers(orgId: String): AdminResult<List<ReportReviewer>>

    suspend fun setReviewers(orgId: String, userIds: List<String>): AdminResult<Unit>
}
