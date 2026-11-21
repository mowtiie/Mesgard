package com.mowtiie.mesgard.domain.model

internal const val REPORT_NOTE_MAX_LENGTH = 1000

internal enum class ReportCategory {
    BULLYING,
    HARASSMENT,
    CHILD_SAFETY,
    OTHER,
}

/** A report about one message. The phone chooses [reportId], so sending again does not create a second report. */
internal data class MessageReport(
    val reportId: String,
    val conversationId: String,
    val messageId: String,
    val category: ReportCategory,
    val note: String,
    val isAnonymous: Boolean,
)

internal enum class ReportError {
    NETWORK_UNAVAILABLE,
    NOT_ALLOWED,
    ALREADY_REPORTED,
    UNKNOWN,
}

internal sealed interface ReportResult {
    data object Sent : ReportResult

    data class Failure(val error: ReportError) : ReportResult
}
