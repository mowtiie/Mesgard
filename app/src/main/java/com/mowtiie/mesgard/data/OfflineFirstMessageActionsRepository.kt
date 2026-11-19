package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.local.upsert
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.ReceiptDto
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.MessageReader
import com.mowtiie.mesgard.domain.model.ReadersResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MESSAGE_READERS_LIMIT
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
import java.io.File
import java.time.Clock
import javax.inject.Inject

private const val TAG = "MessageActions"

private fun ReceiptDto.toReader() = MessageReader(
    userId = userId,
    displayName = profiles?.displayName.orEmpty(),
    readAtEpochMillis = parseEpochMillis(readAt),
)

internal class OfflineFirstMessageActionsRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: MessageRemoteSource,
    private val clock: Clock,
) : MessageActionsRepository {
    private val messageDao = database.messageDao()

    override suspend fun readers(messageId: String): ReadersResult =
        when (val outcome = attemptRemote(TAG) { remoteSource.fetchReaders(messageId, MESSAGE_READERS_LIMIT) }) {
            is RemoteOutcome.Success -> ReadersResult.Loaded(outcome.value.map { it.toReader() })
            is RemoteOutcome.Failure -> ReadersResult.Failure(outcome.error)
        }

    override suspend fun edit(messageId: String, body: String): RefreshResult {
        val text = body.trim()
        val original = messageDao.findById(messageId) ?: return RefreshResult.Failure(RefreshError.UNKNOWN)
        val shown = original.copy(body = text, editedAtEpochMillis = clock.millis())
        messageDao.upsert(shown)
        val outcome = attemptRemote(TAG) { database.storeMessages(listOf(remoteSource.edit(messageId, text))) }
        if (outcome is RemoteOutcome.Failure) putBack(original, shown)
        return outcome.toRefreshResult()
    }

    override suspend fun delete(messageId: String): RefreshResult {
        val original = messageDao.findById(messageId) ?: return RefreshResult.Failure(RefreshError.UNKNOWN)
        val localPaths = database.attachmentDao().findByMessage(messageId).mapNotNull { it.localPath }
        val shown = original.copy(body = "", deletedAtEpochMillis = clock.millis())
        messageDao.upsert(shown)
        val outcome = attemptRemote(TAG) {
            database.storeMessages(listOf(remoteSource.delete(messageId)))
            localPaths.forEach { File(it).delete() }
        }
        if (outcome is RemoteOutcome.Failure) putBack(original, shown)
        return outcome.toRefreshResult()
    }

    /** Takes back the change that was shown at once, unless the message has changed again since, for example live. */
    private suspend fun putBack(original: MessageEntity, shown: MessageEntity) {
        if (messageDao.findById(original.id) == shown) messageDao.upsert(original)
    }
}
