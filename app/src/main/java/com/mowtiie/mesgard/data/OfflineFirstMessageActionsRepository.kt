package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
import java.io.File
import javax.inject.Inject

private const val TAG = "MessageActions"

internal class OfflineFirstMessageActionsRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: MessageRemoteSource,
) : MessageActionsRepository {
    override suspend fun delete(messageId: String): RefreshResult {
        val localPaths = database.attachmentDao().findByMessage(messageId).mapNotNull { it.localPath }
        return attemptRemote(TAG) {
            database.storeMessages(listOf(remoteSource.delete(messageId)))
            localPaths.forEach { File(it).delete() }
        }.toRefreshResult()
    }
}
