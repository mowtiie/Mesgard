package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult

/** Which group a group conversation belongs to. It needs a connection and anyone in the conversation can ask. */
internal interface ConversationGroupRepository {
    suspend fun groupOf(conversationId: String): AdminResult<String>
}
