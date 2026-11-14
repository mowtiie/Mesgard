package com.mowtiie.mesgard.domain.model

/** One recorded admin action. [details] holds what the trigger stored, such as a role or a group name. */
internal data class AuditEntry(
    val id: String,
    val actorId: String,
    val action: String,
    val targetId: String?,
    val details: Map<String, String>,
    val createdAtEpochMillis: Long,
)

/** Where the next page starts: after this entry in the order newest first. */
internal data class AuditCursor(val createdAt: String, val id: String)

internal data class AuditPage(val entries: List<AuditEntry>, val next: AuditCursor?)
