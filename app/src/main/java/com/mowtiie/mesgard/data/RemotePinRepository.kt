package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.PinRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.PinnedMessage
import com.mowtiie.mesgard.domain.model.PinsOutcome
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.PinRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "PinRepository"

internal class RemotePinRepository @Inject constructor(
    private val remote: PinRemoteSource,
    database: MesgardDatabase,
    private val accountRepository: AccountRepository,
) : PinRepository {
    private val conversationDao = database.conversationDao()

    override suspend fun fetch(conversationId: String): PinsOutcome =
        when (val outcome = attemptRemote(TAG) { remote.fetch(conversationId) }) {
            is RemoteOutcome.Success -> PinsOutcome.Loaded(
                outcome.value.map { PinnedMessage(it.messageId, it.messages.sender.displayName, it.messages.body) },
            )
            is RemoteOutcome.Failure -> PinsOutcome.Failure(outcome.error)
        }

    override fun observeCanPin(conversationId: String): Flow<Boolean> =
        accountRepository.observeAccount().map { account ->
            val orgId = conversationDao.findOrgId(conversationId)
            account?.memberships.orEmpty().any { it.organizationId == orgId && it.role in PINNING_ROLES }
        }

    override suspend fun pin(conversationId: String, messageId: String): RefreshResult {
        val orgId = conversationDao.findOrgId(conversationId) ?: return RefreshResult.Failure(RefreshError.UNKNOWN)
        return attemptRemote(TAG) { remote.pin(conversationId, orgId, messageId) }.toRefreshResult()
    }

    override suspend fun unpin(messageId: String): RefreshResult =
        attemptRemote(TAG) { remote.unpin(messageId) }.toRefreshResult()
}

private val PINNING_ROLES = setOf(OrgRole.ORG_ADMIN, OrgRole.STAFF)
