package com.mowtiie.mesgard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mowtiie.mesgard.domain.model.ConversationType

@Entity(tableName = "conversations")
internal data class ConversationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val type: ConversationType,
    val title: String?,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "unread_count") val unreadCount: Int,
    @ColumnInfo(name = "can_post", defaultValue = "1") val canPost: Boolean = true,
    @ColumnInfo(name = "is_muted", defaultValue = "0") val isMuted: Boolean = false,
    @ColumnInfo(name = "pinned_at") val pinnedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "archived_at") val archivedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "marked_unread", defaultValue = "0") val markedUnread: Boolean = false,
    @ColumnInfo(name = "hidden_before") val hiddenBeforeEpochMillis: Long? = null,
    @ColumnInfo(name = "group_path") val groupPath: String? = null,
)
