package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.GroupRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.data.remote.toGroup
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.Group
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.GroupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "GroupRepository"

internal class OfflineFirstGroupRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: GroupRemoteSource,
    private val conversationRepository: ConversationRepository,
    private val authRepository: AuthRepository,
) : GroupRepository {
    private val groupDao = database.groupDao()

    override fun observeGroups(): Flow<List<Group>> = groupDao.observeAll().map { groups ->
        groups.map { it.toGroup() }
    }

    override suspend fun refresh(): RefreshResult {
        val userId = authRepository.currentUserId ?: return RefreshResult.Failure(RefreshError.UNKNOWN)
        return attemptRemote(TAG) {
            val memberships = remoteSource.fetchMemberships(userId)
            val groups = remoteSource.fetchGroups().map { it.toEntity(memberships) }
            database.withTransaction {
                groupDao.deleteAll()
                groupDao.upsertAll(groups)
            }
        }.toRefreshResult()
    }

    override suspend fun startGroupConversation(group: Group): StartConversationResult {
        val created = attemptRemote(TAG) { remoteSource.createGroupConversation(group.id) }
        return when (created) {
            is RemoteOutcome.Failure -> StartConversationResult.Failure(created.error)
            is RemoteOutcome.Success -> openCreated(created.value)
        }
    }

    private suspend fun openCreated(conversationId: String): StartConversationResult =
        when (val refreshed = conversationRepository.refresh()) {
            RefreshResult.Success -> StartConversationResult.Started(conversationId)
            is RefreshResult.Failure -> StartConversationResult.Failure(refreshed.error)
        }
}
