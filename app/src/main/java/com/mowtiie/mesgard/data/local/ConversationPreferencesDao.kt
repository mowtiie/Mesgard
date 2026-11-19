package com.mowtiie.mesgard.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update

/** What a person chose for a conversation, kept to put it back when the server refuses a change. */
internal data class ConversationPreferencesRow(
    val id: String,
    @ColumnInfo(name = "pinned_at") val pinnedAt: Long?,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?,
    @ColumnInfo(name = "marked_unread") val markedUnread: Boolean,
    @ColumnInfo(name = "is_muted") val isMuted: Boolean,
    @ColumnInfo(name = "hidden_before") val hiddenBefore: Long?,
    @ColumnInfo(name = "unread_count") val unreadCount: Int,
)

/** The choices a person makes about their own conversation list. */
@Dao
internal interface ConversationPreferencesDao {
    @Query("UPDATE conversations SET pinned_at = :pinnedAt WHERE id IN (:conversationIds)")
    suspend fun updatePinned(conversationIds: List<String>, pinnedAt: Long?)

    @Query("UPDATE conversations SET archived_at = :archivedAt WHERE id IN (:conversationIds)")
    suspend fun updateArchived(conversationIds: List<String>, archivedAt: Long?)

    @Query("UPDATE conversations SET is_muted = :isMuted WHERE id IN (:conversationIds)")
    suspend fun updateMuted(conversationIds: List<String>, isMuted: Boolean)

    @Query("UPDATE conversations SET marked_unread = :markedUnread WHERE id IN (:conversationIds)")
    suspend fun updateMarkedUnread(conversationIds: List<String>, markedUnread: Boolean)

    @Query(
        """
        UPDATE conversations SET hidden_before = :hiddenBefore, pinned_at = NULL, archived_at = NULL,
            marked_unread = 0, unread_count = 0
        WHERE id = :conversationId
        """,
    )
    suspend fun hide(conversationId: String, hiddenBefore: Long)

    @Query(
        """
        SELECT id, pinned_at, archived_at, marked_unread, is_muted, hidden_before, unread_count
        FROM conversations WHERE id IN (:conversationIds)
        """,
    )
    suspend fun find(conversationIds: List<String>): List<ConversationPreferencesRow>

    @Update(entity = ConversationEntity::class)
    suspend fun restore(row: ConversationPreferencesRow)

    @Query("SELECT COUNT(*) FROM conversations WHERE pinned_at IS NOT NULL AND id NOT IN (:conversationIds)")
    suspend fun countPinnedExcept(conversationIds: List<String>): Int

    @Query("SELECT MAX(created_at) FROM messages WHERE conversation_id = :conversationId")
    suspend fun findNewestMessageCreatedAt(conversationId: String): Long?
}
