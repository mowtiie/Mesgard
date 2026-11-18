package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.PinsOutcome
import com.mowtiie.mesgard.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

internal const val MAX_PINNED_MESSAGES = 5

internal interface PinRepository {
    /** The pinned messages of the conversation, the latest pinned first. Needs a connection. */
    suspend fun fetch(conversationId: String): PinsOutcome

    /** Whether the current user is an administrator or staff of the organization of the conversation. */
    fun observeCanPin(conversationId: String): Flow<Boolean>

    suspend fun pin(conversationId: String, messageId: String): RefreshResult

    suspend fun unpin(messageId: String): RefreshResult
}
