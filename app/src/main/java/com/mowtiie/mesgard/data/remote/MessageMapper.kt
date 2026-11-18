package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.local.ProfileEntity
import com.mowtiie.mesgard.domain.model.MessageStatus
import java.time.OffsetDateTime

internal fun MessageDto.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    body = body,
    createdAtEpochMillis = parseEpochMillis(createdAt),
    status = MessageStatus.SENT,
    editedAtEpochMillis = editedAt?.let(::parseEpochMillis),
    deletedAtEpochMillis = deletedAt?.let(::parseEpochMillis),
    replyToId = replyToMessageId,
    mentionedUserIds = mentionedUserIds.joinIds(),
)

internal fun MessageDto.toSenderProfile(): ProfileEntity? =
    sender?.let { ProfileEntity(id = it.id, displayName = it.displayName) }

internal fun MessageEntity.toNewMessageDto(orgId: String) = NewMessageDto(
    id = id,
    orgId = orgId,
    conversationId = conversationId,
    senderId = senderId,
    body = body,
    replyToMessageId = replyToId,
    mentionedUserIds = mentionedUserIds.splitIds(),
)

private const val ID_SEPARATOR = ","

/** Mentions are stored as one text column, because a message mentions at most 20 people and is read as a whole. */
internal fun List<String>.joinIds(): String? = takeIf { it.isNotEmpty() }?.joinToString(ID_SEPARATOR)

internal fun String?.splitIds(): List<String> = this?.split(ID_SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

internal fun parseEpochMillis(timestamp: String): Long = OffsetDateTime.parse(timestamp).toInstant().toEpochMilli()
