package com.mowtiie.mesgard.domain.model

/**
 * Someone an organization removed. [canErase] is false once erased, and for a person who belongs to another
 * organization.
 */
internal data class FormerMember(
    val userId: String,
    val displayName: String,
    val removedAtEpochMillis: Long,
    val isErased: Boolean,
    val canErase: Boolean,
)

/** Where the next page starts, with the server's own timestamp so no precision is lost. */
internal data class FormerMemberCursor(val removedAt: String, val userId: String)

internal data class FormerMembersPage(val members: List<FormerMember>, val next: FormerMemberCursor?)
