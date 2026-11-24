package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.UsernameRemoteSource
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.UsernamePolicy
import com.mowtiie.mesgard.domain.model.UsernameRules
import com.mowtiie.mesgard.domain.repository.UsernameRepository
import javax.inject.Inject

internal class RemoteUsernameRepository @Inject constructor(private val remote: UsernameRemoteSource) :
    UsernameRepository {
    override suspend fun rules(orgId: String): AdminResult<UsernameRules> = attemptAdmin {
        val dto = remote.rules(orgId)
        UsernameRules(
            orgId = orgId,
            policy = usernamePolicyOf(dto.policy),
            username = dto.username,
            policyAllowsChange = dto.policyAllows,
            nextChangeAtEpochMillis = dto.nextChangeAt?.let(::parseEpochMillis),
        )
    }

    override suspend fun change(orgId: String, username: String): AdminResult<Unit> =
        attemptAdmin(conflict = AdminError.USERNAME_TAKEN) { remote.change(orgId, username.trim().lowercase()) }

    override suspend fun setPolicy(orgId: String, policy: UsernamePolicy): AdminResult<Unit> =
        attemptAdmin { remote.setPolicy(orgId, policy.name.lowercase()) }

    override suspend fun reset(orgId: String, userId: String): AdminResult<Unit> =
        attemptAdmin { remote.reset(orgId, userId) }
}

/** Anything the app does not know counts as the strictest policy, so a change is never offered on a guess. */
internal fun usernamePolicyOf(wireName: String): UsernamePolicy =
    UsernamePolicy.entries.firstOrNull { it.name.lowercase() == wireName } ?: UsernamePolicy.NOBODY
