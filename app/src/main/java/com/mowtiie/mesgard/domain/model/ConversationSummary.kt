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
    /** Where the group of a group conversation sits, such as the grade it is inside. Null at the top. */
    val groupPath: String? = null,
    /** A direct conversation with a person who is a favorite. */
    val isWithFavorite: Boolean = false,
)
