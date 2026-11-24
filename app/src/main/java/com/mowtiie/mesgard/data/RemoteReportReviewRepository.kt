package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.ReportDecisionRequest
import com.mowtiie.mesgard.data.remote.ReportReviewRemoteSource
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ReportCursor
import com.mowtiie.mesgard.domain.model.ReportDecision
import com.mowtiie.mesgard.domain.model.ReportDetail
import com.mowtiie.mesgard.domain.model.ReportPage
import com.mowtiie.mesgard.domain.model.ReportReviewer
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.repository.REPORT_PAGE_SIZE
import com.mowtiie.mesgard.domain.repository.ReportReviewRepository
import javax.inject.Inject

internal class RemoteReportReviewRepository @Inject constructor(private val remote: ReportReviewRemoteSource) :
    ReportReviewRepository {
    override suspend fun canReview(orgId: String): AdminResult<Boolean> = attemptAdmin { remote.canReview(orgId) }

    override suspend fun list(orgId: String, status: ReportStatus, before: ReportCursor?): AdminResult<ReportPage> =
        attemptAdmin {
            val rows = remote.list(orgId, status.wireName(), before, REPORT_PAGE_SIZE)
            val last = rows.lastOrNull()
            val isFullPage = rows.size == REPORT_PAGE_SIZE
            ReportPage(
                reports = rows.map { it.toSummary() },
                next = last?.takeIf { isFullPage }?.let { ReportCursor(it.createdAt, it.reportId) },
            )
        }

    override suspend fun detail(reportId: String): AdminResult<ReportDetail> =
        attemptAdmin { remote.detail(reportId).toDetail() }

    override suspend fun decide(reportId: String, decision: ReportDecision): AdminResult<Unit> = attemptAdmin<Unit> {
        remote.decide(
            ReportDecisionRequest(
                reportId = reportId,
                status = decision.status.wireName(),
                note = decision.note.trim().ifEmpty { null },
                removeMessage = decision.removeMessage,
                resetUsername = decision.resetUsername,
            ),
        )
    }

    override suspend fun revealReporter(reportId: String, reason: String): AdminResult<String> =
        attemptAdmin { remote.revealReporter(reportId, reason.trim()).reporterName.orEmpty() }

    override suspend fun reviewers(orgId: String): AdminResult<List<ReportReviewer>> = attemptAdmin {
        remote.reviewers(orgId).map { ReportReviewer(it.userId, it.displayName) }
    }

    override suspend fun setReviewers(orgId: String, userIds: List<String>): AdminResult<Unit> =
        attemptAdmin<Unit> { remote.setReviewers(orgId, userIds) }
}
