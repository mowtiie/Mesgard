package com.mowtiie.mesgard.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConversationSummaryDto(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("org_id") val orgId: String,
    val type: String,
    val title: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("can_post") val canPost: Boolean = true,
    @SerialName("last_message_id") val lastMessageId: String? = null,
    @SerialName("last_message_sender_id") val lastMessageSenderId: String? = null,
    @SerialName("last_message_body") val lastMessageBody: String? = null,
    @SerialName("last_message_created_at") val lastMessageCreatedAt: String? = null,
)
