package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.GroupCreationRemoteSource
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupCreationPolicy
import com.mowtiie.mesgard.domain.model.GroupCreationRules
import com.mowtiie.mesgard.domain.repository.GroupCreationRepository
import javax.inject.Inject

internal class RemoteGroupCreationRepository @Inject constructor(private val remote: GroupCreationRemoteSource) :
    GroupCreationRepository {
    override suspend fun rules(orgId: String): AdminResult<GroupCreationRules> = attemptAdmin {
        val rules = remote.rules(orgId)
        GroupCreationRules(rules.policy.toPolicy(), rules.canCreate, rules.created, rules.limit)
    }

    override suspend fun setPolicy(orgId: String, policy: GroupCreationPolicy): AdminResult<Unit> =
        attemptAdmin { remote.setPolicy(orgId, policy.toWireName()) }
}

private fun String.toPolicy() = GroupCreationPolicy.entries.firstOrNull { it.toWireName() == this }
    ?: GroupCreationPolicy.ADMINS_ONLY

private fun GroupCreationPolicy.toWireName() = name.lowercase()
