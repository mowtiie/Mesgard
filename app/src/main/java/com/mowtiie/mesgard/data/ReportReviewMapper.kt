package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.EvidenceDto
import com.mowtiie.mesgard.data.remote.EvidenceFileDto
import com.mowtiie.mesgard.data.remote.EvidenceLineDto
import com.mowtiie.mesgard.data.remote.ReportDetailDto
import com.mowtiie.mesgard.data.remote.ReportEventDto
import com.mowtiie.mesgard.data.remote.ReportRowDto
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.domain.model.EvidenceFile
import com.mowtiie.mesgard.domain.model.EvidenceLine
import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportDetail
import com.mowtiie.mesgard.domain.model.ReportEvent
import com.mowtiie.mesgard.domain.model.ReportEventType
import com.mowtiie.mesgard.domain.model.ReportEvidence
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.model.ReportSummary

internal fun ReportStatus.wireName(): String = when (this) {
    ReportStatus.OPEN -> "open"
    ReportStatus.IN_REVIEW -> "in_review"
    ReportStatus.RESOLVED -> "resolved"
    ReportStatus.DISMISSED -> "dismissed"
}

private fun reportStatusOf(wireName: String): ReportStatus =
    ReportStatus.entries.firstOrNull { it.wireName() == wireName } ?: ReportStatus.OPEN

private fun reportCategoryOf(wireName: String): ReportCategory =
    ReportCategory.entries.firstOrNull { it.wireName() == wireName } ?: ReportCategory.OTHER

private fun reportEventTypeOf(wireName: String): ReportEventType = when (wireName) {
    "submitted" -> ReportEventType.SUBMITTED
    "viewed" -> ReportEventType.VIEWED
    "status_changed" -> ReportEventType.STATUS_CHANGED
    "message_removed" -> ReportEventType.MESSAGE_REMOVED
    "reporter_revealed" -> ReportEventType.REPORTER_REVEALED
    else -> ReportEventType.OTHER
}

internal fun ReportRowDto.toSummary() = ReportSummary(
    id = reportId,
    category = reportCategoryOf(category),
    status = reportStatusOf(status),
    subjectName = subjectName,
    isAnonymous = anonymous,
    reporterName = reporterName,
    createdAtEpochMillis = parseEpochMillis(createdAt),
)

internal fun ReportDetailDto.toDetail() = ReportDetail(
    id = id,
    category = reportCategoryOf(category),
    status = reportStatusOf(status),
    note = note,
    isAnonymous = anonymous,
    reporterName = reporterName,
    subjectName = subjectName,
    isMessageRemoved = messageRemoved,
    resolutionNote = resolutionNote,
    createdAtEpochMillis = parseEpochMillis(createdAt),
    evidence = evidence?.toEvidence(),
    files = files.map { it.toFile() },
    events = events.map { it.toEvent() },
)

private fun EvidenceDto.toEvidence() = ReportEvidence(
    conversationTitle = conversationTitle,
    message = EvidenceLine(senderName, body, parseEpochMillis(sentAt)),
    earlier = context.map { it.toLine() },
)

private fun EvidenceLineDto.toLine() = EvidenceLine(senderName, body, parseEpochMillis(sentAt))

private fun EvidenceFileDto.toFile() = EvidenceFile(fileName, mimeType, sizeBytes)

private fun ReportEventDto.toEvent() = ReportEvent(
    type = reportEventTypeOf(action),
    actorName = actorName,
    note = note,
    createdAtEpochMillis = parseEpochMillis(createdAt),
)
