package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.domain.model.ReportCategory
import com.mowtiie.mesgard.domain.model.ReportEventType
import com.mowtiie.mesgard.domain.model.ReportStatus
import com.mowtiie.mesgard.domain.model.ReportSubject

internal fun ReportStatus.wireName(): String = when (this) {
    ReportStatus.OPEN -> "open"
    ReportStatus.IN_REVIEW -> "in_review"
    ReportStatus.RESOLVED -> "resolved"
    ReportStatus.DISMISSED -> "dismissed"
}

internal fun reportStatusOf(wireName: String): ReportStatus =
    ReportStatus.entries.firstOrNull { it.wireName() == wireName } ?: ReportStatus.OPEN

internal fun reportCategoryOf(wireName: String): ReportCategory =
    ReportCategory.entries.firstOrNull { it.wireName() == wireName } ?: ReportCategory.OTHER

internal fun reportSubjectOf(wireName: String): ReportSubject =
    ReportSubject.entries.firstOrNull { it != ReportSubject.UNKNOWN && it.wireName() == wireName }
        ?: ReportSubject.UNKNOWN

internal fun reportEventTypeOf(wireName: String): ReportEventType = when (wireName) {
    "submitted" -> ReportEventType.SUBMITTED
    "viewed" -> ReportEventType.VIEWED
    "status_changed" -> ReportEventType.STATUS_CHANGED
    "message_removed" -> ReportEventType.MESSAGE_REMOVED
    "reporter_revealed" -> ReportEventType.REPORTER_REVEALED
    else -> ReportEventType.OTHER
}
