package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.local.PinnedMessageEntity
import com.mowtiie.mesgard.data.remote.PinRemoteSource
import com.mowtiie.mesgard.data.remote.PinnedRowDto
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.PinnedMessage
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.PinRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "PinRepository"

internal class OfflineFirstPinRepository @Inject constructor(
    private val remote: PinRemoteSource,
    private val database: MesgardDatabase,
    private val accountRepository: AccountRepository,
    private val clock: Clock,
) : PinRepository {
    private val conversationDao = database.conversationDao()
    private val pinDao = database.pinnedMessageDao()

    override fun observePins(conversationId: String): Flow<List<PinnedMessage>> = pinDao.observe(conversationId)
        .map { rows -> rows.map { PinnedMessage(it.messageId, it.senderName, it.body) } }

    override suspend fun refresh(conversationId: String): RefreshResult = attemptRemote(TAG) {
        val pins = remote.fetch(conversationId).map { it.toEntity(conversationId) }
        database.withTransaction {
            pinDao.deleteForConversation(conversationId)
            pinDao.upsertAll(pins)
        }
    }.toRefreshResult()

    override fun observeCanPin(conversationId: String): Flow<Boolean> =
        accountRepository.observeAccount().map { account ->
            val orgId = conversationDao.findOrgId(conversationId)
            account?.memberships.orEmpty().any { it.organizationId == orgId && it.role in PINNING_ROLES }
        }

    override suspend fun pin(conversationId: String, messageId: String): RefreshResult {
        val orgId = conversationDao.findOrgId(conversationId) ?: return RefreshResult.Failure(RefreshError.UNKNOWN)
        shownPin(conversationId, messageId)?.let { pinDao.upsertAll(listOf(it)) }
        val outcome = attemptRemote(TAG) { remote.pin(conversationId, orgId, messageId) }
        if (outcome is RemoteOutcome.Failure) pinDao.delete(messageId)
        return outcome.toRefreshResult()
    }

    override suspend fun unpin(messageId: String): RefreshResult {
        val shown = pinDao.find(messageId)
        pinDao.delete(messageId)
        val outcome = attemptRemote(TAG) { remote.unpin(messageId) }
        if (outcome is RemoteOutcome.Failure && shown != null) pinDao.upsertAll(listOf(shown))
        return outcome.toRefreshResult()
    }

    /** The pin as it will show, made from the stored message. Null when the message is not on the phone. */
    private suspend fun shownPin(conversationId: String, messageId: String): PinnedMessageEntity? {
        val message = database.messageDao().findById(messageId) ?: return null
        val senderName = database.profileDao().findDisplayName(message.senderId).orEmpty()
        return PinnedMessageEntity(messageId, conversationId, clock.millis(), senderName, message.body)
    }
}

private fun PinnedRowDto.toEntity(conversationId: String) = PinnedMessageEntity(
    messageId = messageId,
    conversationId = conversationId,
    pinnedAtEpochMillis = parseEpochMillis(pinnedAt),
    senderName = messages.sender.displayName,
    body = messages.body,
)

private val PINNING_ROLES = setOf(OrgRole.ORG_ADMIN, OrgRole.STAFF)
