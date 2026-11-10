package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mowtiie.mesgard.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MessageDao {
    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query(
        "SELECT * FROM messages WHERE conversation_id = :conversationId " +
            "ORDER BY created_at DESC, id DESC LIMIT :limit",
    )
    fun observeLatest(conversationId: String, limit: Int): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: String, status: MessageStatus)
}
