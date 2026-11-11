package com.mowtiie.mesgard.data

import androidx.room.withTransaction
import com.mowtiie.mesgard.data.local.AccountEntity
import com.mowtiie.mesgard.data.local.AccountMembershipEntity
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.remote.AccountRemoteSource
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toOrgRole
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.AccountMembership
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AccountRepository
import com.mowtiie.mesgard.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val TAG = "AccountRepository"

internal class OfflineFirstAccountRepository @Inject constructor(
    private val database: MesgardDatabase,
    private val remoteSource: AccountRemoteSource,
    private val authRepository: AuthRepository,
) : AccountRepository {
    private val accountDao = database.accountDao()

    override fun observeAccount(): Flow<Account?> =
        combine(accountDao.observeAccount(), accountDao.observeMemberships()) { account, memberships ->
            account?.let {
                Account(
                    displayName = it.displayName,
                    email = it.email,
                    memberships = memberships.map { membership ->
                        AccountMembership(membership.orgId, membership.orgName, membership.role.toOrgRole())
                    },
                )
            }
        }

    override suspend fun refresh(): RefreshResult {
        val userId = authRepository.currentUserId ?: return RefreshResult.Failure(RefreshError.UNKNOWN)
        val email = authRepository.currentUserEmail
        return attemptRemote(TAG) {
            val displayName = requireNotNull(remoteSource.fetchDisplayName(userId)) { "The profile does not exist" }
            val memberships = remoteSource.fetchMemberships(userId).map {
                AccountMembershipEntity(it.orgId, it.organization.name, it.role)
            }
            database.withTransaction {
                accountDao.deleteAccounts()
                accountDao.deleteMemberships()
                accountDao.upsertAccount(AccountEntity(userId, displayName, email))
                accountDao.upsertMemberships(memberships)
            }
        }.toRefreshResult()
    }
}
