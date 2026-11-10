package com.mowtiie.mesgard.domain.model

internal data class ConversationSummary(
    val id: String,
    val type: ConversationType,
    val title: String?,
    val lastMessagePreview: String?,
    val lastActivityAtEpochMillis: Long,
    val unreadCount: Int,
    val canPost: Boolean,
)
