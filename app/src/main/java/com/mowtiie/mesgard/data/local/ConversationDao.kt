package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    val isMuted: Boolean,
    val pinnedAtEpochMillis: Long?,
    val archivedAtEpochMillis: Long?,
    val markedUnread: Boolean,
    val lastMessageBody: String?,
    val activityAtEpochMillis: Long,
)

private const val CONVERSATION_ITEM_SELECT = """
    SELECT c.id AS id, c.type AS type, c.title AS title, c.unread_count AS unreadCount,
        c.can_post AS canPost, c.is_muted AS isMuted,
        c.pinned_at AS pinnedAtEpochMillis, c.archived_at AS archivedAtEpochMillis,
        c.marked_unread AS markedUnread,
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
        WHERE c.archived_at IS NULL
            AND (c.hidden_before IS NULL OR m.created_at > c.hidden_before)
        ORDER BY (c.pinned_at IS NULL), c.pinned_at DESC, activityAtEpochMillis DESC, c.id DESC
        LIMIT :limit
        """,
    )
    fun observeList(limit: Int): Flow<List<ConversationListItem>>

    @Query(
        """
        $CONVERSATION_ITEM_SELECT
        WHERE c.archived_at IS NOT NULL
            AND (c.hidden_before IS NULL OR m.created_at > c.hidden_before)
        ORDER BY activityAtEpochMillis DESC, c.id DESC
        LIMIT :limit
        """,
    )
    fun observeArchived(limit: Int): Flow<List<ConversationListItem>>

    @Query("$CONVERSATION_ITEM_SELECT WHERE c.id = :conversationId")
    fun observeById(conversationId: String): Flow<ConversationListItem?>

    @Query(
        """
        SELECT c.id FROM conversations AS c
        WHERE c.unread_count > 0 AND c.archived_at IS NULL
        ORDER BY (SELECT MAX(created_at) FROM messages WHERE conversation_id = c.id) DESC, c.id DESC
        LIMIT :limit
        """,
    )
    suspend fun findRecentIdsWithUnread(limit: Int): List<String>

    @Query("SELECT org_id FROM conversations WHERE id = :conversationId")
    suspend fun findOrgId(conversationId: String): String?

    @Query("UPDATE conversations SET unread_count = unread_count + 1 WHERE id = :conversationId")
    suspend fun incrementUnreadCount(conversationId: String)

    @Query("UPDATE conversations SET unread_count = 0 WHERE id = :conversationId")
    suspend fun clearUnreadCount(conversationId: String)

    @Upsert
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    /** A conversation the phone does not have yet. One it has keeps its pin, unread count and the rest. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id NOT IN (:keptIds)")
    suspend fun deleteAllExcept(keptIds: List<String>)
}
