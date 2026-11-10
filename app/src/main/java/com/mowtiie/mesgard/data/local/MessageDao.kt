package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mowtiie.mesgard.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

internal data class TimelineMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String?,
    val body: String,
    val createdAtEpochMillis: Long,
    val editedAtEpochMillis: Long?,
    val deletedAtEpochMillis: Long?,
    val status: MessageStatus,
)

@Dao
internal interface MessageDao {
    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE conversation_id NOT IN (:keptConversationIds)")
    suspend fun deleteOutsideConversations(keptConversationIds: List<String>)

    @Query(
        """
        SELECT m.id AS id, m.conversation_id AS conversationId, m.sender_id AS senderId,
            p.display_name AS senderName, m.body AS body, m.created_at AS createdAtEpochMillis,
            m.edited_at AS editedAtEpochMillis, m.deleted_at AS deletedAtEpochMillis, m.status AS status
        FROM messages AS m
        LEFT JOIN profiles AS p ON p.id = m.sender_id
        WHERE m.conversation_id = :conversationId
        ORDER BY m.created_at DESC, m.id DESC
        LIMIT :limit
        """,
    )
    fun observeTimeline(conversationId: String, limit: Int): Flow<List<TimelineMessage>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun findById(messageId: String): MessageEntity?

    @Query("SELECT MIN(created_at) FROM messages WHERE conversation_id = :conversationId AND status = 'SENT'")
    suspend fun findOldestSentCreatedAt(conversationId: String): Long?

    @Query(
        """
        SELECT id FROM messages
        WHERE conversation_id = :conversationId AND status = 'SENT' AND sender_id <> :currentUserId
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun findNewestIncomingId(conversationId: String, currentUserId: String): String?

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET status = 'SENT', created_at = :createdAtEpochMillis WHERE id = :messageId")
    suspend fun markSent(messageId: String, createdAtEpochMillis: Long)
}
