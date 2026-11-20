package com.mowtiie.mesgard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The other person of a direct conversation. It is kept apart from the conversation row because a refresh replaces
 * that row, and the summaries from the server do not say who the other person is.
 */
@Entity(tableName = "direct_peers")
internal data class DirectPeerEntity(
    @PrimaryKey @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "user_id") val userId: String,
)
