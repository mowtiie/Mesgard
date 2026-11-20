package com.mowtiie.mesgard.ui.chats

import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.RefreshError

/** [conversations] is already filtered by [query]; [totalConversations] counts them without the filter. */
internal data class ConversationListUiState(
    val conversations: List<ConversationSummary> = emptyList(),
    val totalConversations: Int = 0,
    val archivedCount: Int = 0,
    /** Who is typing in each conversation, for the conversations at the top of the list. */
    val typing: Map<String, List<String>> = emptyMap(),
    val query: String = "",
    val filter: ConversationFilter = ConversationFilter.ALL,
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

    val showArchivedRow: Boolean
        get() = archivedCount > 0 && !isFiltering

    val showNoResults: Boolean
        get() = isFiltering && conversations.isEmpty()

    val showFilters: Boolean
        get() = totalConversations > 0

    /** A chip other than All matches nothing while the search is empty. */
    val showFilterEmpty: Boolean
        get() = filter != ConversationFilter.ALL && !isFiltering && conversations.isEmpty() && totalConversations > 0
}
