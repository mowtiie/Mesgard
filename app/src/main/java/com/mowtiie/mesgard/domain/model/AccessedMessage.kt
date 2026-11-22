package com.mowtiie.mesgard.domain.model

/** A message read through break-glass access. Deleted messages have no text, and files are only marked. */
internal data class AccessedMessage(
    val id: String,
    val senderName: String?,
    val body: String?,
    val createdAtEpochMillis: Long,
    /** The time as the server wrote it, so the next page can start exactly after this message. */
    val cursorCreatedAt: String,
    val isDeleted: Boolean,
    val hasAttachments: Boolean,
)
