package com.mowtiie.mesgard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A pinned message as the pin bar shows it, with the text and the sender copied so it shows without the message. */
@Entity(
    tableName = "pinned_messages",
    indices = [Index(value = ["conversation_id", "pinned_at"])],
)
internal data class PinnedMessageEntity(
    @PrimaryKey @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "pinned_at") val pinnedAtEpochMillis: Long,
    @ColumnInfo(name = "sender_name") val senderName: String,
    val body: String,
)
