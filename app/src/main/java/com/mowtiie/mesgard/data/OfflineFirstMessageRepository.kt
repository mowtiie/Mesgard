package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.attachment.AttachmentIntake
import com.mowtiie.mesgard.data.attachment.IntakeResult
import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.local.MessageDao
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.local.TimelineMessage
import com.mowtiie.mesgard.data.local.upsert
import com.mowtiie.mesgard.data.remote.MessageDto
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.joinIds
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.data.remote.splitIds
import com.mowtiie.mesgard.data.remote.toDomain
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.data.work.MessageSendScheduler
import com.mowtiie.mesgard.domain.attachment.MAX_ATTACHMENTS_PER_MESSAGE
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.domain.model.PageResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.ReplyPreview
import com.mowtiie.mesgard.domain.model.SendRejection
import com.mowtiie.mesgard.domain.model.SendResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.MAX_MESSAGE_LENGTH
import com.mowtiie.mesgard.domain.repository.MESSAGE_PAGE_SIZE
import com.mowtiie.mesgard.domain.repository.MessageRepository
import java.io.File
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

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
        database.profileDao().observeAll(),
        database.contactDao().observeAll(),
    ) { items, attachments, profiles, contacts ->
        val currentUserId = authRepository.currentUserId
        val attachmentsByMessage = attachments.groupBy { it.messageId }
        val names = contacts.associate { it.userId to it.displayName } + profiles.associate { it.id to it.displayName }
        items.map { toMessage(it, currentUserId, attachmentsByMessage[it.id].orEmpty(), names) }
    }.distinctUntilChanged()

    override fun observeNewestIncomingId(conversationId: String): Flow<String?> =
        authRepository.currentUserId?.let { messageDao.observeNewestIncomingId(conversationId, it) } ?: flowOf(null)

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
                database.storeMessages(older)
                older.size < MESSAGE_PAGE_SIZE
            }
        }
        return when (outcome) {
            is RemoteOutcome.Success -> PageResult.Loaded(endReached = outcome.value)
            is RemoteOutcome.Failure -> PageResult.Failure(outcome.error)
        }
    }

    override suspend fun send(
        conversationId: String,
        body: String,
        attachmentUris: List<String>,
        replyToId: String?,
        mentionedUserIds: List<String>,
    ): SendResult {
        val text = body.trim()
        val senderId = authRepository.currentUserId
        val rejection = rejectionFor(
            text,
            attachmentUris.size,
            senderId,
            conversationDao.findOrgId(conversationId) != null,
        )
        return if (rejection != null || senderId == null) {
            SendResult.Rejected(rejection ?: SendRejection.NOT_SIGNED_IN)
        } else {
            queue(
                NewMessage(UUID.randomUUID().toString(), conversationId, senderId, text, replyToId, mentionedUserIds),
                attachmentUris,
            )
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
            replyToId = draft.replyToId,
            mentionedUserIds = draft.mentionedUserIds.joinIds(),
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

    override suspend fun discard(messageId: String) {
        val localPaths = attachmentDao.findByMessage(messageId).mapNotNull { it.localPath }
        if (messageDao.deleteFailed(messageId) > 0) localPaths.forEach { File(it).delete() }
    }

    override suspend fun markConversationRead(conversationId: String) {
        conversationDao.clearUnreadCount(conversationId)
        sendScheduler.scheduleReadMarker(conversationId)
    }

    private suspend fun storeLatestPage(conversationId: String, page: List<MessageDto>) {
        database.withTransaction {
            val gapStart = messageDao.findGapStart(page)
            database.storeMessages(page)
            if (gapStart != null) messageDao.deleteSentOlderThan(conversationId, gapStart)
        }
    }
}

/**
 * A full page whose oldest message is already stored continues the stored history, so the history stays.
 * Otherwise messages may be missing before the page, and the older ones are dropped to keep it contiguous.
 */
private suspend fun MessageDao.findGapStart(page: List<MessageDto>): Long? {
    if (page.size < MESSAGE_PAGE_SIZE) return null
    val oldest = page.minBy { parseEpochMillis(it.createdAt) }
    return if (findById(oldest.id) == null) parseEpochMillis(oldest.createdAt) else null
}

private fun toMessage(
    item: TimelineMessage,
    currentUserId: String?,
    attachments: List<AttachmentEntity>,
    names: Map<String, String>,
) = Message(
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
    replyTo = item.replyToId?.let { replyPreviewOf(it, item) },
    mentionNames = item.mentionedUserIds.splitIds().mapNotNull { names[it] },
    mentionsYou = currentUserId != null && currentUserId in item.mentionedUserIds.splitIds(),
)

private fun replyPreviewOf(messageId: String, item: TimelineMessage) = ReplyPreview(
    messageId = messageId,
    isLoaded = item.replyOriginalId != null,
    senderName = item.replySenderName,
    body = item.replyBody.orEmpty(),
    isDeleted = item.replyDeletedAtEpochMillis != null,
)

private data class NewMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val replyToId: String?,
    val mentionedUserIds: List<String>,
)

private fun rejectionFor(
    text: String,
    attachmentCount: Int,
    senderId: String?,
    conversationExists: Boolean,
): SendRejection? = when {
    text.isEmpty() && attachmentCount == 0 -> SendRejection.EMPTY_MESSAGE
    text.length > MAX_MESSAGE_LENGTH -> SendRejection.MESSAGE_TOO_LONG
    attachmentCount > MAX_ATTACHMENTS_PER_MESSAGE -> SendRejection.TOO_MANY_ATTACHMENTS
    senderId == null -> SendRejection.NOT_SIGNED_IN
    !conversationExists -> SendRejection.CONVERSATION_UNAVAILABLE
    else -> null
}
