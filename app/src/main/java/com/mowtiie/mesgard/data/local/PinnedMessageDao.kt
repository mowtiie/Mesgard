package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PinnedMessageDao {
    @Query("SELECT * FROM pinned_messages WHERE conversation_id = :conversationId ORDER BY pinned_at DESC, message_id")
    fun observe(conversationId: String): Flow<List<PinnedMessageEntity>>

    @Query("SELECT * FROM pinned_messages WHERE message_id = :messageId")
    suspend fun find(messageId: String): PinnedMessageEntity?

    @Upsert
    suspend fun upsertAll(pins: List<PinnedMessageEntity>)

    @Query("DELETE FROM pinned_messages WHERE message_id = :messageId")
    suspend fun delete(messageId: String)

    @Query("DELETE FROM pinned_messages WHERE conversation_id = :conversationId")
    suspend fun deleteForConversation(conversationId: String)
}
