package com.mowtiie.mesgard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mowtiie.mesgard.domain.model.MessageStatus

@Entity(
    tableName = "messages",
    indices = [
        Index(
            value = ["conversation_id", "created_at"],
            orders = [Index.Order.ASC, Index.Order.DESC],
        ),
    ],
)
internal data class MessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "sender_id") val senderId: String,
    val body: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    val status: MessageStatus,
    @ColumnInfo(name = "edited_at") val editedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "deleted_at") val deletedAtEpochMillis: Long? = null,
)
