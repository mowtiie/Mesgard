package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mowtiie.mesgard.domain.model.ConversationType
import kotlinx.coroutines.flow.Flow

internal data class ConversationListItem(
    val id: String,
    val type: ConversationType,
    val title: String?,
    val unreadCount: Int,
    val canPost: Boolean,
    val lastMessageBody: String?,
    val activityAtEpochMillis: Long,
)

private const val CONVERSATION_ITEM_SELECT = """
    SELECT c.id AS id, c.type AS type, c.title AS title, c.unread_count AS unreadCount,
        c.can_post AS canPost,
        m.body AS lastMessageBody, COALESCE(m.created_at, c.created_at) AS activityAtEpochMillis
    FROM conversations AS c
    LEFT JOIN messages AS m ON m.id = (
        SELECT id FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC, id DESC LIMIT 1
    )
"""

@Dao
internal interface ConversationDao {
    @Query(
        """
        $CONVERSATION_ITEM_SELECT
        ORDER BY (c.type = 'ANNOUNCEMENT') DESC, activityAtEpochMillis DESC, c.id DESC
        LIMIT :limit
        """,
    )
    fun observeList(limit: Int): Flow<List<ConversationListItem>>

    @Query("$CONVERSATION_ITEM_SELECT WHERE c.id = :conversationId")
    fun observeById(conversationId: String): Flow<ConversationListItem?>

    @Query("SELECT org_id FROM conversations WHERE id = :conversationId")
    suspend fun findOrgId(conversationId: String): String?

    @Query("UPDATE conversations SET unread_count = unread_count + 1 WHERE id = :conversationId")
    suspend fun incrementUnreadCount(conversationId: String)

    @Query("UPDATE conversations SET unread_count = 0 WHERE id = :conversationId")
    suspend fun clearUnreadCount(conversationId: String)

    @Upsert
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query("DELETE FROM conversations WHERE id NOT IN (:keptIds)")
    suspend fun deleteAllExcept(keptIds: List<String>)
}
