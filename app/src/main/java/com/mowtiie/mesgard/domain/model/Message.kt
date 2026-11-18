package com.mowtiie.mesgard.domain.model

internal data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String?,
    val body: String,
    val createdAtEpochMillis: Long,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val status: MessageStatus,
    val isOwn: Boolean,
    val attachments: List<Attachment> = emptyList(),
    val replyTo: ReplyPreview? = null,
    val mentionNames: List<String> = emptyList(),
    val mentionsYou: Boolean = false,
)
