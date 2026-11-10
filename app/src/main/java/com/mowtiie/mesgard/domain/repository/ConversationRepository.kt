package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

internal interface ConversationRepository {
    fun observeConversations(): Flow<List<ConversationSummary>>

    suspend fun refresh(): RefreshResult
}
