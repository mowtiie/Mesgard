package com.mowtiie.mesgard.domain.model

import com.mowtiie.mesgard.domain.avatar.AvatarRef

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
    /** The picture of the group of a group conversation, null when it has none. */
    val groupAvatar: AvatarRef? = null,
    /** The group of a group conversation, which a member can report. */
    val groupId: String? = null,
    /** A direct conversation with a person who is a favorite. */
    val isWithFavorite: Boolean = false,
)
