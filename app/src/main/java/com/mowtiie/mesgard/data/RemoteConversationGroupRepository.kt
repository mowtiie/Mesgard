package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.ConversationGroupRemoteSource
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.repository.ConversationGroupRepository
import javax.inject.Inject

internal class RemoteConversationGroupRepository @Inject constructor(
    private val remote: ConversationGroupRemoteSource,
) : ConversationGroupRepository {
    override suspend fun groupOf(conversationId: String): AdminResult<String> = attemptAdmin {
        checkNotNull(remote.findGroupId(conversationId)) { "The conversation has no group" }
    }
}
