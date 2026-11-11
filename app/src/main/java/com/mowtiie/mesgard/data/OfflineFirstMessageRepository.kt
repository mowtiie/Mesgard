package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.attachment.AttachmentIntake
import com.mowtiie.mesgard.data.attachment.IntakeResult
import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.local.TimelineMessage
import com.mowtiie.mesgard.data.local.upsertKeepingLocalPaths
import com.mowtiie.mesgard.data.remote.MessageDto
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.data.remote.toDomain
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.data.remote.toSenderProfile
import com.mowtiie.mesgard.data.work.MessageSendScheduler
import com.mowtiie.mesgard.domain.attachment.MAX_ATTACHMENTS_PER_MESSAGE
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.domain.model.PageResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.SendRejection
import com.mowtiie.mesgard.domain.model.SendResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MAX_MESSAGE_LENGTH
import com.mowtiie.mesgard.domain.repository.MESSAGE_PAGE_SIZE
import com.mowtiie.mesgard.domain.repository.MessageRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val TAG = "MessageRepository"

internal class OfflineFirstMessageRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: MessageRemoteSource,
    private val sendScheduler: MessageSendScheduler,
    private val attachmentIntake: AttachmentIntake,
    private val authRepository: AuthRepository,
    private val clock: Clock,
) : MessageRepository {
    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val profileDao = database.profileDao()
    private val attachmentDao = database.attachmentDao()

    override fun observeMessages(conversationId: String, limit: Int): Flow<List<Message>> = combine(
        messageDao.observeTimeline(conversationId, limit),
        attachmentDao.observeForConversation(conversationId),
    ) { items, attachments ->
        val currentUserId = authRepository.currentUserId
        val attachmentsByMessage = attachments.groupBy { it.messageId }
        items.map { toMessage(it, currentUserId, attachmentsByMessage[it.id].orEmpty()) }
    }

    override suspend fun refresh(conversationId: String): RefreshResult = attemptRemote(TAG) {
        storeLatestPage(conversationId, remoteSource.fetchLatest(conversationId))
    }.toRefreshResult()

    override suspend fun loadOlder(conversationId: String): PageResult {
        val oldest = messageDao.findOldestSentCreatedAt(conversationId)
        val outcome = attemptRemote(TAG) {
            if (oldest == null) {
                val latest = remoteSource.fetchLatest(conversationId)
                storeLatestPage(conversationId, latest)
                latest.size < MESSAGE_PAGE_SIZE
            } else {
                val older = remoteSource.fetchOlder(conversationId, beforeEpochMillis = oldest + 1)
                storeMessages(older)
                older.size < MESSAGE_PAGE_SIZE
            }
        }
        return when (outcome) {
            is RemoteOutcome.Success -> PageResult.Loaded(endReached = outcome.value)
            is RemoteOutcome.Failure -> PageResult.Failure(outcome.error)
        }
    }

    override suspend fun send(conversationId: String, body: String, attachmentUris: List<String>): SendResult {
        val text = body.trim()
        val senderId = authRepository.currentUserId
        val rejection = rejectionFor(text, attachmentUris.size, senderId, conversationId)
        return if (rejection != null || senderId == null) {
            SendResult.Rejected(rejection ?: SendRejection.NOT_SIGNED_IN)
        } else {
            queue(NewMessage(UUID.randomUUID().toString(), conversationId, senderId, text), attachmentUris)
        }
    }

    private suspend fun queue(draft: NewMessage, attachmentUris: List<String>): SendResult {
        val attachments = when (val intake = attachmentIntake.prepare(draft.conversationId, draft.id, attachmentUris)) {
            is IntakeResult.Rejected -> return SendResult.Rejected(intake.reason)
            is IntakeResult.Ready -> intake.attachments
        }
        val message = MessageEntity(
            id = draft.id,
            conversationId = draft.conversationId,
            senderId = draft.senderId,
            body = draft.body,
            createdAtEpochMillis = clock.millis(),
            status = MessageStatus.PENDING,
        )
        database.withTransaction {
            messageDao.upsert(message)
            attachmentDao.upsertAll(attachments)
        }
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

    private suspend fun rejectionFor(
        text: String,
        attachmentCount: Int,
        senderId: String?,
        conversationId: String,
    ): SendRejection? = when {
        text.isEmpty() && attachmentCount == 0 -> SendRejection.EMPTY_MESSAGE
        text.length > MAX_MESSAGE_LENGTH -> SendRejection.MESSAGE_TOO_LONG
        attachmentCount > MAX_ATTACHMENTS_PER_MESSAGE -> SendRejection.TOO_MANY_ATTACHMENTS
        senderId == null -> SendRejection.NOT_SIGNED_IN
        conversationDao.findOrgId(conversationId) == null -> SendRejection.CONVERSATION_UNAVAILABLE
        else -> null
    }

    private suspend fun storeLatestPage(conversationId: String, page: List<MessageDto>) {
        database.withTransaction {
            storeMessages(page)
            if (page.size >= MESSAGE_PAGE_SIZE) {
                messageDao.deleteSentOlderThan(conversationId, page.minOf { parseEpochMillis(it.createdAt) })
            }
        }
    }

    private suspend fun storeMessages(messages: List<MessageDto>) {
        database.withTransaction {
            profileDao.upsertAll(messages.mapNotNull { it.toSenderProfile() }.distinctBy { it.id })
            messageDao.upsertAll(messages.map { it.toEntity() })
            attachmentDao.upsertKeepingLocalPaths(
                messages.flatMap { dto ->
                    dto.attachments.map { it.toEntity(dto.id) }
                },
            )
        }
    }
}

private fun toMessage(item: TimelineMessage, currentUserId: String?, attachments: List<AttachmentEntity>) = Message(
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
    attachments = attachments.map { it.toDomain() },
)

private data class NewMessage(val id: String, val conversationId: String, val senderId: String, val body: String)
