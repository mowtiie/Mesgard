package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.local.TimelineMessage
import com.mowtiie.mesgard.data.remote.MESSAGE_PAGE_SIZE
import com.mowtiie.mesgard.data.remote.MessageDto
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.data.remote.toSenderProfile
import com.mowtiie.mesgard.data.work.MessageSendScheduler
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.domain.model.PageResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.SendRejection
import com.mowtiie.mesgard.domain.model.SendResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MAX_MESSAGE_LENGTH
import com.mowtiie.mesgard.domain.repository.MessageRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "MessageRepository"

internal class OfflineFirstMessageRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: MessageRemoteSource,
    private val sendScheduler: MessageSendScheduler,
    private val authRepository: AuthRepository,
    private val clock: Clock,
) : MessageRepository {
    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val profileDao = database.profileDao()

    override fun observeMessages(conversationId: String, limit: Int): Flow<List<Message>> =
        messageDao.observeTimeline(conversationId, limit).map { items ->
            val currentUserId = authRepository.currentUserId
            items.map { toMessage(it, currentUserId) }
        }

    override suspend fun refresh(conversationId: String): RefreshResult =
        attemptRemote(TAG) { storeMessages(remoteSource.fetchLatest(conversationId)) }.toRefreshResult()

    override suspend fun loadOlder(conversationId: String): PageResult {
        val oldest = messageDao.findOldestSentCreatedAt(conversationId)
        val outcome = attemptRemote(TAG) {
            val page = if (oldest == null) {
                remoteSource.fetchLatest(conversationId)
            } else {
                remoteSource.fetchOlder(conversationId, beforeEpochMillis = oldest + 1)
            }
            storeMessages(page)
            page.size < MESSAGE_PAGE_SIZE
        }
        return when (outcome) {
            is RemoteOutcome.Success -> PageResult.Loaded(endReached = outcome.value)
            is RemoteOutcome.Failure -> PageResult.Failure(outcome.error)
        }
    }

    override suspend fun send(conversationId: String, body: String): SendResult {
        val text = body.trim()
        val senderId = authRepository.currentUserId
        val rejection = rejectionFor(text, senderId, conversationId)
        if (rejection != null || senderId == null) {
            return SendResult.Rejected(rejection ?: SendRejection.NOT_SIGNED_IN)
        }
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId,
            body = text,
            createdAtEpochMillis = clock.millis(),
            status = MessageStatus.PENDING,
        )
        messageDao.upsert(message)
        sendScheduler.schedule(message.id)
        return SendResult.Queued
    }

    override suspend fun retry(messageId: String) {
        messageDao.updateStatus(messageId, MessageStatus.PENDING)
        sendScheduler.schedule(messageId)
    }

    override suspend fun markConversationRead(conversationId: String): RefreshResult {
        conversationDao.clearUnreadCount(conversationId)
        val newestIncomingId = authRepository.currentUserId
            ?.let { messageDao.findNewestIncomingId(conversationId, it) }
        if (newestIncomingId == null) return RefreshResult.Success
        return attemptRemote(TAG) {
            remoteSource.markConversationRead(conversationId, newestIncomingId)
        }.toRefreshResult()
    }

    private suspend fun rejectionFor(text: String, senderId: String?, conversationId: String): SendRejection? = when {
        text.isEmpty() -> SendRejection.EMPTY_MESSAGE
        text.length > MAX_MESSAGE_LENGTH -> SendRejection.MESSAGE_TOO_LONG
        senderId == null -> SendRejection.NOT_SIGNED_IN
        conversationDao.findOrgId(conversationId) == null -> SendRejection.CONVERSATION_UNAVAILABLE
        else -> null
    }

    private suspend fun storeMessages(messages: List<MessageDto>) {
        database.withTransaction {
            profileDao.upsertAll(messages.mapNotNull { it.toSenderProfile() }.distinctBy { it.id })
            messageDao.upsertAll(messages.map { it.toEntity() })
        }
    }

    private fun toMessage(item: TimelineMessage, currentUserId: String?) = Message(
        id = item.id,
        conversationId = item.conversationId,
        senderId = item.senderId,
        senderName = item.senderName,
        body = item.body,
        createdAtEpochMillis = item.createdAtEpochMillis,
        isEdited = item.editedAtEpochMillis != null,
        isDeleted = item.deletedAtEpochMillis != null,
        status = item.status,
        isOwn = item.senderId == currentUserId,
    )
}
