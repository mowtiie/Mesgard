package com.mowtiie.mesgard.testing

import com.mowtiie.mesgard.data.local.ConversationEntity
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.remote.ConversationSummaryDto
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.MessageStatus

internal const val TEST_ORG_ID = "org"
internal const val TEST_SENDER_ID = "sender"

internal fun messageEntity(
    id: String,
    conversationId: String,
    createdAt: Long,
    body: String = "Hello",
    status: MessageStatus = MessageStatus.SENT,
) = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = TEST_SENDER_ID,
    body = body,
    createdAtEpochMillis = createdAt,
    status = status,
)

internal fun conversationEntity(
    id: String,
    type: ConversationType = ConversationType.DIRECT,
    title: String? = "Title $id",
    createdAt: Long = 0L,
    unreadCount: Int = 0,
) = ConversationEntity(
    id = id,
    orgId = TEST_ORG_ID,
    type = type,
    title = title,
    createdAtEpochMillis = createdAt,
    unreadCount = unreadCount,
)

internal fun summaryDto(
    id: String,
    type: String = "direct",
    lastMessageBody: String? = "Last message of $id",
    lastMessageCreatedAt: String? = "2026-11-10T08:00:00+00:00",
) = ConversationSummaryDto(
    conversationId = id,
    orgId = TEST_ORG_ID,
    type = type,
    title = "Title $id",
    createdAt = "2026-11-01T08:00:00+00:00",
    unreadCount = 2,
    lastMessageId = lastMessageBody?.let { "message-$id" },
    lastMessageSenderId = lastMessageBody?.let { TEST_SENDER_ID },
    lastMessageBody = lastMessageBody,
    lastMessageCreatedAt = lastMessageCreatedAt,
)
