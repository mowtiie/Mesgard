package com.mowtiie.mesgard.domain.model

internal enum class ReportStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    DISMISSED,
    ;

    val isClosed: Boolean
        get() = this == RESOLVED || this == DISMISSED
}

/** One line of the list of reports. A reporter who chose to stay anonymous has no name here. */
internal data class ReportSummary(
    val id: String,
    val category: ReportCategory,
    val subject: ReportSubject,
    val status: ReportStatus,
    val subjectName: String,
    val isAnonymous: Boolean,
    val reporterName: String?,
    val createdAtEpochMillis: Long,
)

internal data class ReportCursor(val createdAt: String, val id: String)

internal data class ReportPage(val reports: List<ReportSummary>, val next: ReportCursor?)

internal data class EvidenceLine(val senderName: String, val body: String, val sentAtEpochMillis: Long)

/** The copy of the message kept when it was reported. It does not change when the sender edits or deletes it. */
internal data class ReportEvidence(
    val conversationTitle: String?,
    val message: EvidenceLine,
    val earlier: List<EvidenceLine>,
)

internal data class EvidenceFile(val fileName: String, val mimeType: String, val sizeBytes: Long)

internal enum class ReportEventType {
    SUBMITTED,
    VIEWED,
    STATUS_CHANGED,
    MESSAGE_REMOVED,
    REPORTER_REVEALED,
    OTHER,
}

internal data class ReportEvent(
    val type: ReportEventType,
    val actorName: String?,
    val note: String?,
    val createdAtEpochMillis: Long,
)

internal data class ReportDetail(
    val id: String,
    val category: ReportCategory,
    val subject: ReportSubject,
    val status: ReportStatus,
    val note: String?,
    val isAnonymous: Boolean,
    val reporterName: String?,
    val subjectName: String,
    /** The text of a name, as it was when it was reported. */
    val subjectText: String?,
    val isMessageRemoved: Boolean,
    val resolutionNote: String?,
    val createdAtEpochMillis: Long,
    val evidence: ReportEvidence?,
    val files: List<EvidenceFile>,
    val events: List<ReportEvent>,
)

/** What a reviewer decides. Closing a report needs a note, and a message can be removed or a username reset with it. */
internal data class ReportDecision(
    val status: ReportStatus,
    val note: String,
    val removeMessage: Boolean,
    val resetUsername: Boolean = false,
)

internal data class ReportReviewer(val userId: String, val displayName: String)
