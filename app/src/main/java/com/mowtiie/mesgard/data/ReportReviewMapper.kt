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
import com.mowtiie.mesgard.domain.model.ReportDetail
import com.mowtiie.mesgard.domain.model.ReportEvent
import com.mowtiie.mesgard.domain.model.ReportEvidence
import com.mowtiie.mesgard.domain.model.ReportSummary

internal fun ReportRowDto.toSummary() = ReportSummary(
    id = reportId,
    category = reportCategoryOf(category),
    subject = reportSubjectOf(subject),
    status = reportStatusOf(status),
    subjectName = subjectName,
    isAnonymous = anonymous,
    reporterName = reporterName,
    createdAtEpochMillis = parseEpochMillis(createdAt),
)

internal fun ReportDetailDto.toDetail() = ReportDetail(
    id = id,
    category = reportCategoryOf(category),
    subject = reportSubjectOf(subject),
    status = reportStatusOf(status),
    note = note,
    isAnonymous = anonymous,
    reporterName = reporterName,
    subjectName = subjectName,
    subjectText = subjectText,
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

private fun EvidenceFileDto.toFile() = EvidenceFile(fileName, mimeType, sizeBytes, storagePath)

private fun ReportEventDto.toEvent() = ReportEvent(
    type = reportEventTypeOf(action),
    actorName = actorName,
    note = note,
    createdAtEpochMillis = parseEpochMillis(createdAt),
)
