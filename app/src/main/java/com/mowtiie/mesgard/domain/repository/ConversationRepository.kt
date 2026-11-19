package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

internal interface ConversationRepository {
    fun observeConversations(): Flow<List<ConversationSummary>>

    /** The conversations the person archived. They are not part of [observeConversations]. */
    fun observeArchived(): Flow<List<ConversationSummary>>

    fun observeConversation(conversationId: String): Flow<ConversationSummary?>

    suspend fun refresh(): RefreshResult

    /**
     * Refreshes the list and then the latest messages of the most recent conversations with unread messages, so they
     * open without a gap after the connection was down.
     */
    suspend fun refreshAfterReconnect(): RefreshResult
}
