package com.mowtiie.mesgard.domain.model

internal data class ConversationSummary(
    val id: String,
    val type: ConversationType,
    val title: String?,
    val lastMessagePreview: String?,
    val lastActivityAtEpochMillis: Long,
    val unreadCount: Int,
    val canPost: Boolean,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMarkedUnread: Boolean = false,
    /** A direct conversation with a person who is a favorite. */
    val isWithFavorite: Boolean = false,
)
