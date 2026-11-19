package com.mowtiie.mesgard.ui.announcements

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.RefreshError

internal data class AnnouncementsUiState(
    val conversationId: String? = null,
    val announcements: List<Message> = emptyList(),
    val unreadCount: Int = 0,
    val canPost: Boolean = false,
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshError: RefreshError? = null,
) {
    val showEmptyMessage: Boolean
        get() = isLoaded && announcements.isEmpty() && !isRefreshing
}
