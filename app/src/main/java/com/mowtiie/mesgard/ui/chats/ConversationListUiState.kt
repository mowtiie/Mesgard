package com.mowtiie.mesgard.ui.chats

import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.RefreshError

internal data class ConversationListUiState(
    val conversations: List<ConversationSummary> = emptyList(),
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshError: RefreshError? = null,
) {
    val showEmptyMessage: Boolean
        get() = isLoaded && conversations.isEmpty() && !isRefreshing
}
