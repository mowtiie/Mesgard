package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import kotlinx.coroutines.flow.Flow

internal interface GroupRepository {
    fun observeGroups(): Flow<List<Group>>

    suspend fun refresh(): RefreshResult

    suspend fun startGroupConversation(group: Group): StartConversationResult
}
