package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.ConversationListItem
import com.mowtiie.mesgard.data.local.DirectPeerEntity
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.CONVERSATION_PAGE_SIZE
import com.mowtiie.mesgard.data.remote.ConversationRemoteSource
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.data.remote.toLastMessageEntity
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.di.ApplicationScope
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.MessageRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

private const val TAG = "ConversationRepository"
private const val CATCH_UP_LIMIT = 10
private const val SHARING_TIMEOUT_MS = 5_000L

internal class OfflineFirstConversationRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: ConversationRemoteSource,
    private val authRepository: AuthRepository,
    private val messageRepository: MessageRepository,
    @ApplicationScope scope: CoroutineScope,
) : ConversationRepository {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val directPeerDao = database.directPeerDao()

    // The list, the selection bar and the archive watch the same queries, so each runs once for all of them.
    private val favoriteIds = directPeerDao.observeFavoriteConversationIds()
    private val conversations = shared(scope, conversationDao.observeList(CONVERSATION_PAGE_SIZE))
    private val archived = shared(scope, conversationDao.observeArchived(CONVERSATION_PAGE_SIZE))

    override fun observeConversations(): Flow<List<ConversationSummary>> = conversations

    override fun observeArchived(): Flow<List<ConversationSummary>> = archived

    override fun observeConversation(conversationId: String): Flow<ConversationSummary?> =
        conversationDao.observeById(conversationId).map { item -> item?.let { toSummary(it, isWithFavorite = false) } }

    override suspend fun refresh(): RefreshResult = attemptRemote(TAG) { storeSummaries() }.toRefreshResult()

    override suspend fun refreshAfterReconnect(): RefreshResult {
        val result = refresh()
        if (result is RefreshResult.Success) {
            conversationDao.findRecentIdsWithUnread(CATCH_UP_LIMIT).forEach { messageRepository.refresh(it) }
        }
        return result
    }

    private suspend fun storeSummaries() {
        // Both are asked at once, so the refresh waits for one round trip instead of two.
        val (summaries, muted) = coroutineScope {
            val summaries = async { remoteSource.fetchSummaries() }
            val muted =
                async { authRepository.currentUserId?.let { remoteSource.fetchMutedConversationIds(it) }.orEmpty() }
            summaries.await() to muted.await()
        }
        val conversations = summaries.map { it.toEntity(isMuted = it.conversationId in muted) }
        val lastMessages = summaries.mapNotNull { it.toLastMessageEntity() }
        database.withTransaction {
            conversationDao.upsertAll(conversations)
            messageDao.upsertAll(lastMessages)
            if (summaries.size < CONVERSATION_PAGE_SIZE) removeConversationsNoLongerShared(conversations.map { it.id })
        }
        storeMissingPeers(conversations.filter { it.type == ConversationType.DIRECT }.map { it.id })
    }

    /** The other person of a direct conversation never changes, so each one is asked for once. */
    private suspend fun storeMissingPeers(directIds: List<String>) {
        val userId = authRepository.currentUserId ?: return
        val missing = directIds - directPeerDao.findKnownConversationIds(directIds).toSet()
        if (missing.isEmpty()) return
        val peers = remoteSource.fetchDirectPeerIds(missing, userId)
        directPeerDao.upsertAll(peers.map { (conversationId, peerId) -> DirectPeerEntity(conversationId, peerId) })
    }

    private suspend fun removeConversationsNoLongerShared(currentIds: List<String>) {
        conversationDao.deleteAllExcept(currentIds)
        messageDao.deleteOutsideConversations(currentIds)
    }

    private fun shared(
        scope: CoroutineScope,
        items: Flow<List<ConversationListItem>>,
    ): Flow<List<ConversationSummary>> = combine(items, favoriteIds) { list, favorites ->
        val favoriteSet = favorites.toSet()
        list.map { toSummary(it, it.id in favoriteSet) }
    }.shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = SHARING_TIMEOUT_MS,
            replayExpirationMillis = 0,
        ),
        replay = 1,
    )

    private fun toSummary(item: ConversationListItem, isWithFavorite: Boolean) = ConversationSummary(
        id = item.id,
        type = item.type,
        title = item.title,
        lastMessagePreview = item.lastMessageBody,
        lastActivityAtEpochMillis = item.activityAtEpochMillis,
        unreadCount = item.unreadCount,
        canPost = item.canPost,
        isMuted = item.isMuted,
        isPinned = item.pinnedAtEpochMillis != null,
        isArchived = item.archivedAtEpochMillis != null,
        isMarkedUnread = item.markedUnread,
        groupPath = item.groupPath,
        isWithFavorite = isWithFavorite,
    )
}
