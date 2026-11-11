package com.mowtiie.mesgard.testing

import com.mowtiie.mesgard.data.local.ContactEntity
import com.mowtiie.mesgard.data.local.ConversationEntity
import com.mowtiie.mesgard.data.local.GroupEntity
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.remote.ContactDto
import com.mowtiie.mesgard.data.remote.ConversationSummaryDto
import com.mowtiie.mesgard.data.remote.GroupDto
import com.mowtiie.mesgard.data.remote.MessageDto
import com.mowtiie.mesgard.data.remote.SenderDto
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

internal fun messageDto(
    id: String,
    conversationId: String,
    senderId: String = TEST_SENDER_ID,
    createdAt: String = "2026-11-10T08:00:00+00:00",
    senderName: String? = "Ana",
    deletedAt: String? = null,
) = MessageDto(
    id = id,
    orgId = TEST_ORG_ID,
    conversationId = conversationId,
    senderId = senderId,
    body = if (deletedAt == null) "Body of $id" else "",
    createdAt = createdAt,
    deletedAt = deletedAt,
    sender = senderName?.let { SenderDto(id = senderId, displayName = it) },
)

internal fun contactDto(userId: String, name: String = "Name $userId", role: String = "student") = ContactDto(
    orgId = TEST_ORG_ID,
    userId = userId,
    role = role,
    user = SenderDto(id = userId, displayName = name),
)

internal fun contactEntity(userId: String, name: String = "Name $userId", role: String = "student") = ContactEntity(
    orgId = TEST_ORG_ID,
    userId = userId,
    displayName = name,
    role = role,
)

internal fun groupDto(id: String, name: String = "Name $id") = GroupDto(id = id, orgId = TEST_ORG_ID, name = name)

internal fun groupEntity(id: String, name: String = "Name $id", canStartConversation: Boolean = true) = GroupEntity(
    id = id,
    orgId = TEST_ORG_ID,
    name = name,
    canStartConversation = canStartConversation,
)
