package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.data.local.ConversationEntity
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.MessageStatus

internal fun ConversationSummaryDto.toEntity() = ConversationEntity(
    id = conversationId,
    orgId = orgId,
    type = ConversationType.valueOf(type.uppercase()),
    title = title,
    createdAtEpochMillis = parseEpochMillis(createdAt),
    unreadCount = unreadCount,
    canPost = canPost,
)

internal fun ConversationSummaryDto.toLastMessageEntity(): MessageEntity? {
    if (lastMessageId == null) return null
    return MessageEntity(
        id = lastMessageId,
        conversationId = conversationId,
        senderId = requireNotNull(lastMessageSenderId) { "A last message requires a sender" },
        body = requireNotNull(lastMessageBody) { "A last message requires a body" },
        createdAtEpochMillis = parseEpochMillis(
            requireNotNull(lastMessageCreatedAt) { "A last message requires a timestamp" },
        ),
        status = MessageStatus.SENT,
    )
}
