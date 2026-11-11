package com.mowtiie.mesgard.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MessageDto(
    val id: String,
    @SerialName("org_id") val orgId: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("has_attachments") val hasAttachments: Boolean = false,
    val attachments: List<AttachmentDto> = emptyList(),
    val sender: SenderDto? = null,
)

@Serializable
internal data class SenderDto(val id: String, @SerialName("display_name") val displayName: String)

@Serializable
internal data class NewMessageDto(
    val id: String,
    @SerialName("org_id") val orgId: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
)
