package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.MentionCandidate

internal const val MENTION_SUGGESTION_LIMIT = 6
internal const val MAX_MENTIONS_PER_MESSAGE = 20

internal interface MentionRepository {
    /** People who wrote in the conversation whose name contains [query]. Instant and works without a connection. */
    suspend fun searchStored(conversationId: String, query: String): List<MentionCandidate>

    /**
     * People of the conversation whose name contains [query], without the current user. Empty when the server cannot
     * be reached, because a mention is a convenience and never a reason to stop someone from writing.
     */
    suspend fun search(conversationId: String, query: String): List<MentionCandidate>
}
