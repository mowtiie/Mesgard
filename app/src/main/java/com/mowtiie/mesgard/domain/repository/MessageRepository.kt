package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.PageResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.SendResult
import kotlinx.coroutines.flow.Flow

internal const val MAX_MESSAGE_LENGTH = 4000

internal interface MessageRepository {
    fun observeMessages(conversationId: String, limit: Int): Flow<List<Message>>

    suspend fun refresh(conversationId: String): RefreshResult

    suspend fun loadOlder(conversationId: String): PageResult

    suspend fun send(conversationId: String, body: String): SendResult

    suspend fun retry(messageId: String)

    suspend fun markConversationRead(conversationId: String): RefreshResult
}
