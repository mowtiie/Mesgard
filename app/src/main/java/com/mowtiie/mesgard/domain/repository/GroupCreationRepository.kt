package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupCreationPolicy
import com.mowtiie.mesgard.domain.model.GroupCreationRules

/** Who may create groups. It needs a connection. Any member can read it, only an organization admin can set it. */
internal interface GroupCreationRepository {
    suspend fun rules(orgId: String): AdminResult<GroupCreationRules>

    suspend fun setPolicy(orgId: String, policy: GroupCreationPolicy): AdminResult<Unit>
}
