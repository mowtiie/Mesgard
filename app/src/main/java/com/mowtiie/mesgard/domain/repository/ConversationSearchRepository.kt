package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.SearchCursor
import com.mowtiie.mesgard.domain.model.SearchOutcome

internal const val SEARCH_MIN_LENGTH = 2
internal const val SEARCH_PAGE_SIZE = 30

internal interface ConversationSearchRepository {
    /** The newest messages of the conversation that contain [query], or the page after [before]. Needs a connection. */
    suspend fun search(conversationId: String, query: String, before: SearchCursor?): SearchOutcome
}
