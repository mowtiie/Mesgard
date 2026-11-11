package com.mowtiie.mesgard.ui.chats

import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.RefreshError

/** [conversations] is already filtered by [query]; [totalConversations] counts them without the filter. */
internal data class ConversationListUiState(
    val conversations: List<ConversationSummary> = emptyList(),
    val totalConversations: Int = 0,
    val query: String = "",
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshError: RefreshError? = null,
) {
    val isFiltering: Boolean
        get() = query.isNotBlank()

    val showSearch: Boolean
        get() = totalConversations > 0 || isFiltering

    val showEmptyMessage: Boolean
        get() = isLoaded && totalConversations == 0 && !isRefreshing

    val showNoResults: Boolean
        get() = isFiltering && conversations.isEmpty()
}
