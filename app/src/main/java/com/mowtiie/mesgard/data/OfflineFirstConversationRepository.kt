package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.ConversationListItem
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.CONVERSATION_PAGE_SIZE
import com.mowtiie.mesgard.data.remote.ConversationRemoteSource
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.data.remote.toLastMessageEntity
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "ConversationRepository"

internal class OfflineFirstConversationRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: ConversationRemoteSource,
) : ConversationRepository {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()

    override fun observeConversations(): Flow<List<ConversationSummary>> =
        conversationDao.observeList(CONVERSATION_PAGE_SIZE).map { items -> items.map(::toSummary) }

    override fun observeConversation(conversationId: String): Flow<ConversationSummary?> =
        conversationDao.observeById(conversationId).map { item -> item?.let(::toSummary) }

    override suspend fun refresh(): RefreshResult = attemptRemote(TAG) { storeSummaries() }.toRefreshResult()

    private suspend fun storeSummaries() {
        val summaries = remoteSource.fetchSummaries()
        val conversations = summaries.map { it.toEntity() }
        val lastMessages = summaries.mapNotNull { it.toLastMessageEntity() }
        database.withTransaction {
            conversationDao.upsertAll(conversations)
            messageDao.upsertAll(lastMessages)
            if (summaries.size < CONVERSATION_PAGE_SIZE) removeConversationsNoLongerShared(conversations.map { it.id })
        }
    }

    private suspend fun removeConversationsNoLongerShared(currentIds: List<String>) {
        conversationDao.deleteAllExcept(currentIds)
        messageDao.deleteOutsideConversations(currentIds)
    }

    private fun toSummary(item: ConversationListItem) = ConversationSummary(
        id = item.id,
        type = item.type,
        title = item.title,
        lastMessagePreview = item.lastMessageBody,
        lastActivityAtEpochMillis = item.activityAtEpochMillis,
        unreadCount = item.unreadCount,
    )
}
