package com.mowtiie.mesgard.domain.model

internal const val REPORT_NOTE_MAX_LENGTH = 1000

internal enum class ReportCategory {
    BULLYING,
    HARASSMENT,
    CHILD_SAFETY,
    IMPERSONATION,
    OFFENSIVE_CONTENT,
    OTHER,
    ;

    /** The categories a person can choose for [subject]. Impersonation and offensive content describe a name. */
    companion object {
        fun forSubject(subject: ReportSubject): List<ReportCategory> = when (subject) {
            ReportSubject.NAME, ReportSubject.USERNAME -> listOf(IMPERSONATION, OFFENSIVE_CONTENT, OTHER)
            ReportSubject.PROFILE_PICTURE -> listOf(IMPERSONATION, OFFENSIVE_CONTENT, CHILD_SAFETY, OTHER)
            ReportSubject.MESSAGE, ReportSubject.PERSON -> listOf(BULLYING, HARASSMENT, CHILD_SAFETY, OTHER)
            ReportSubject.UNKNOWN -> listOf(OTHER)
        }
    }
}

/** What a report is about. [UNKNOWN] is a subject a newer server knows and this version does not. */
internal enum class ReportSubject {
    MESSAGE,
    PERSON,
    NAME,
    USERNAME,
    PROFILE_PICTURE,
    UNKNOWN,
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

/** A report about a person: [subject] is their conduct or their name. */
internal data class PersonReport(
    val reportId: String,
    val orgId: String,
    val userId: String,
    val subject: ReportSubject,
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
