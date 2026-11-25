package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.GroupReport
import com.mowtiie.mesgard.domain.model.MessageReport
import com.mowtiie.mesgard.domain.model.PersonReport
import com.mowtiie.mesgard.domain.model.ReportResult

internal interface ReportRepository {
    /** Sends the report to the reviewers of the organization. It needs a connection and is never kept on the phone. */
    suspend fun reportMessage(report: MessageReport): ReportResult

    /** Sends the report about a person to the reviewers of the organization, in the same way. */
    suspend fun reportPerson(report: PersonReport): ReportResult

    /** Sends the report about the picture or the name of a group to the reviewers, in the same way. */
    suspend fun reportGroup(report: GroupReport): ReportResult
}
