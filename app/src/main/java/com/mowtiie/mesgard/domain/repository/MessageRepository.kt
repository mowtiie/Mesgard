package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.PageResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.SendResult
import kotlinx.coroutines.flow.Flow

internal const val MAX_MESSAGE_LENGTH = 4000
internal const val MESSAGE_PAGE_SIZE = 50

internal interface MessageRepository {
    fun observeMessages(conversationId: String, limit: Int): Flow<List<Message>>

    /** The newest sent message from someone else, which is what marking the conversation as read goes up to. */
    fun observeNewestIncomingId(conversationId: String): Flow<String?>

    suspend fun refresh(conversationId: String): RefreshResult

    suspend fun loadOlder(conversationId: String): PageResult

    suspend fun send(
        conversationId: String,
        body: String,
        attachmentUris: List<String> = emptyList(),
        replyToId: String? = null,
        mentionedUserIds: List<String> = emptyList(),
    ): SendResult

    suspend fun retry(messageId: String)

    /** Removes a message that failed to send, with its attachment files. Other messages are left alone. */
    suspend fun discard(messageId: String)

    /** Clears the unread count at once and tells the server when there is a connection, even after a restart. */
    suspend fun markConversationRead(conversationId: String)
}
