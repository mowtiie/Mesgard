package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.PinnedMessage
import com.mowtiie.mesgard.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

internal const val MAX_PINNED_MESSAGES = 5

internal interface PinRepository {
    /** The pinned messages of the conversation, the latest pinned first, from the phone. */
    fun observePins(conversationId: String): Flow<List<PinnedMessage>>

    /** Replaces the stored pins of the conversation with the server's. The stored pins stay when it fails. */
    suspend fun refresh(conversationId: String): RefreshResult

    /** Whether the current user is an administrator or staff of the organization of the conversation. */
    fun observeCanPin(conversationId: String): Flow<Boolean>

    /** Shows the pin at once and takes it back when the server refuses. */
    suspend fun pin(conversationId: String, messageId: String): RefreshResult

    /** Removes the pin at once and puts it back when the server refuses. */
    suspend fun unpin(messageId: String): RefreshResult
}
