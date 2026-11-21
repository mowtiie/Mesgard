package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.MessageReport
import com.mowtiie.mesgard.domain.model.ReportResult

internal interface ReportRepository {
    /** Sends the report to the reviewers of the organization. It needs a connection and is never kept on the phone. */
    suspend fun reportMessage(report: MessageReport): ReportResult
}
