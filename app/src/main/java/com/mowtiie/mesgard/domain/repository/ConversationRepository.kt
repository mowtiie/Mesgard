package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

internal interface ConversationRepository {
    fun observeConversations(): Flow<List<ConversationSummary>>

    /** The conversations the person archived. They are not part of [observeConversations]. */
    fun observeArchived(): Flow<List<ConversationSummary>>

    /** The announcement conversation of the organization, which is not part of [observeConversations]. */
    fun observeAnnouncements(): Flow<ConversationSummary?>

    fun observeConversation(conversationId: String): Flow<ConversationSummary?>

    suspend fun refresh(): RefreshResult

    /** Mutes or unmutes the conversation for the current user only. Push skips it while it is muted. */
    suspend fun setMuted(conversationId: String, muted: Boolean): RefreshResult
}
