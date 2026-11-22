package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.GroupAnnouncementRemoteSource
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.GroupAnnouncementRepository
import javax.inject.Inject

internal class RemoteGroupAnnouncementRepository @Inject constructor(
    private val remote: GroupAnnouncementRemoteSource,
) : GroupAnnouncementRepository {
    override suspend fun send(groupId: String, messageId: String, body: String): AdminResult<Int> =
        attemptAdmin { remote.sendToGroupTree(groupId, messageId, body.trim()) }
}
