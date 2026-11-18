package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.MessageSearchRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.SearchHitDto
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.domain.model.SearchCursor
import com.mowtiie.mesgard.domain.model.SearchHit
import com.mowtiie.mesgard.domain.model.SearchOutcome
import com.mowtiie.mesgard.domain.model.SearchPage
import com.mowtiie.mesgard.domain.repository.ConversationSearchRepository
import com.mowtiie.mesgard.domain.repository.SEARCH_PAGE_SIZE
import javax.inject.Inject

private const val TAG = "ConversationSearch"

internal class RemoteConversationSearchRepository @Inject constructor(private val remote: MessageSearchRemoteSource) :
    ConversationSearchRepository {
    override suspend fun search(conversationId: String, query: String, before: SearchCursor?): SearchOutcome =
        when (val outcome = attemptRemote(TAG) { remote.search(conversationId, query, before, SEARCH_PAGE_SIZE) }) {
            is RemoteOutcome.Success -> SearchOutcome.Loaded(pageOf(outcome.value))
            is RemoteOutcome.Failure -> SearchOutcome.Failure(outcome.error)
        }

    private fun pageOf(rows: List<SearchHitDto>): SearchPage {
        val last = rows.lastOrNull()
        return SearchPage(
            hits = rows.map { SearchHit(it.messageId, it.senderName, it.body, parseEpochMillis(it.createdAt)) },
            next = if (rows.size == SEARCH_PAGE_SIZE &&
                last != null
            ) {
                SearchCursor(last.createdAt, last.messageId)
            } else {
                null
            },
        )
    }
}
