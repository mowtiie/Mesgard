package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.ConversationPreferencesRow
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.ConversationPreferencesRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.domain.model.MAX_PINNED_CONVERSATIONS
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ConversationPreferencesRepository
import com.mowtiie.mesgard.domain.repository.MessageRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val TAG = "ConversationPreferences"

internal class OfflineFirstConversationPreferencesRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: ConversationPreferencesRemoteSource,
    private val authRepository: AuthRepository,
    private val messageRepository: MessageRepository,
) : ConversationPreferencesRepository {
    private val dao = database.conversationPreferencesDao()

    override suspend fun setPinned(conversationIds: Set<String>, pinned: Boolean): PreferenceResult {
        if (pinned && wouldExceedPinLimit(conversationIds)) {
            return PreferenceResult.Failure(PreferenceError.TOO_MANY_PINNED)
        }
        val pinnedAt = if (pinned) System.currentTimeMillis() else null
        return change(conversationIds, mapOf("pinned_at" to timestamp(pinnedAt))) {
            dao.updatePinned(conversationIds.toList(), pinnedAt)
        }
    }

    override suspend fun setArchived(conversationIds: Set<String>, archived: Boolean): PreferenceResult {
        val archivedAt = if (archived) System.currentTimeMillis() else null
        return change(conversationIds, mapOf("archived_at" to timestamp(archivedAt))) {
            dao.updateArchived(conversationIds.toList(), archivedAt)
        }
    }

    override suspend fun setMuted(conversationIds: Set<String>, muted: Boolean): PreferenceResult =
        change(conversationIds, mapOf("muted" to JsonPrimitive(muted))) {
            dao.updateMuted(conversationIds.toList(), muted)
        }

    override suspend fun markUnread(conversationIds: Set<String>): PreferenceResult =
        change(conversationIds, mapOf("marked_unread" to JsonPrimitive(true))) {
            dao.updateMarkedUnread(conversationIds.toList(), true)
        }

    override suspend fun markRead(conversationIds: Set<String>): PreferenceResult {
        val flagResult = change(conversationIds, mapOf("marked_unread" to JsonPrimitive(false))) {
            dao.updateMarkedUnread(conversationIds.toList(), false)
        }
        if (flagResult is PreferenceResult.Failure) return flagResult
        val failure = conversationIds
            .map { messageRepository.markConversationRead(it) }
            .filterIsInstance<RefreshResult.Failure>()
            .firstOrNull()
        return failure?.let { PreferenceResult.Failure(it.error.toPreferenceError()) } ?: PreferenceResult.Success
    }

    override suspend fun deleteForMe(conversationIds: Set<String>): PreferenceResult {
        val userId = authRepository.currentUserId ?: return PreferenceResult.Failure(PreferenceError.UNKNOWN)
        val snapshot = dao.find(conversationIds.toList())
        val hiddenAt = conversationIds.associateWith { id ->
            dao.findNewestMessageCreatedAt(id) ?: System.currentTimeMillis()
        }
        database.withTransaction { hiddenAt.forEach { (id, at) -> dao.hide(id, at) } }
        val outcome = attemptRemote(TAG) {
            hiddenAt.forEach { (id, at) -> remoteSource.update(userId, listOf(id), deleteChanges(at)) }
        }
        return finish(outcome, snapshot)
    }

    private suspend fun change(
        conversationIds: Set<String>,
        changes: Map<String, JsonElement>,
        applyLocally: suspend () -> Unit,
    ): PreferenceResult {
        val userId = authRepository.currentUserId ?: return PreferenceResult.Failure(PreferenceError.UNKNOWN)
        val snapshot = dao.find(conversationIds.toList())
        applyLocally()
        val outcome = attemptRemote(TAG) { remoteSource.update(userId, conversationIds, JsonObject(changes)) }
        return finish(outcome, snapshot)
    }

    private suspend fun finish(outcome: RemoteOutcome<Unit>, snapshot: List<ConversationPreferencesRow>) =
        when (outcome) {
            is RemoteOutcome.Success -> PreferenceResult.Success
            is RemoteOutcome.Failure -> {
                database.withTransaction { snapshot.forEach { dao.restore(it) } }
                PreferenceResult.Failure(outcome.error.toPreferenceError())
            }
        }

    private suspend fun wouldExceedPinLimit(conversationIds: Set<String>): Boolean =
        dao.countPinnedExcept(conversationIds.toList()) + conversationIds.size > MAX_PINNED_CONVERSATIONS

    private fun deleteChanges(hiddenAt: Long): JsonObject = JsonObject(
        mapOf(
            "hidden_before" to timestamp(hiddenAt),
            "pinned_at" to JsonNull,
            "archived_at" to JsonNull,
            "marked_unread" to JsonPrimitive(false),
        ),
    )
}

private fun timestamp(epochMillis: Long?): JsonElement =
    epochMillis?.let { JsonPrimitive(Instant.ofEpochMilli(it).toString()) } ?: JsonNull

private fun RefreshError.toPreferenceError() = when (this) {
    RefreshError.NETWORK_UNAVAILABLE -> PreferenceError.NETWORK_UNAVAILABLE
    RefreshError.UNKNOWN -> PreferenceError.UNKNOWN
}
