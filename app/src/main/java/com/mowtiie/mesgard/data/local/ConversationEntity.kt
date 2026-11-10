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
)
