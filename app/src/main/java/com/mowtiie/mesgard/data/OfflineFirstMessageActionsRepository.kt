package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.ReceiptDto
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.MessageReader
import com.mowtiie.mesgard.domain.model.ReadersResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MESSAGE_READERS_LIMIT
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
import java.io.File
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
) : MessageActionsRepository {
    override suspend fun readers(messageId: String): ReadersResult =
        when (val outcome = attemptRemote(TAG) { remoteSource.fetchReaders(messageId, MESSAGE_READERS_LIMIT) }) {
            is RemoteOutcome.Success -> ReadersResult.Loaded(outcome.value.map { it.toReader() })
            is RemoteOutcome.Failure -> ReadersResult.Failure(outcome.error)
        }

    override suspend fun edit(messageId: String, body: String): RefreshResult = attemptRemote(TAG) {
        database.storeMessages(listOf(remoteSource.edit(messageId, body.trim())))
    }.toRefreshResult()

    override suspend fun delete(messageId: String): RefreshResult {
        val localPaths = database.attachmentDao().findByMessage(messageId).mapNotNull { it.localPath }
        return attemptRemote(TAG) {
            database.storeMessages(listOf(remoteSource.delete(messageId)))
            localPaths.forEach { File(it).delete() }
        }.toRefreshResult()
    }
}
