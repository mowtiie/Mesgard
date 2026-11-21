package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.MessagingRules

/** The messaging rules of an organization. They need a connection and are enforced by the server. */
internal interface MessagingRulesRepository {
    suspend fun rules(orgId: String): AdminResult<MessagingRules>

    /** Only an organization admin can set them. */
    suspend fun setRules(orgId: String, rules: MessagingRules): AdminResult<Unit>
}
