package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.UsernamePolicy
import com.mowtiie.mesgard.domain.model.UsernameRules

/** Usernames of one organization. It needs a connection and is never kept on the phone. */
internal interface UsernameRepository {
    suspend fun rules(orgId: String): AdminResult<UsernameRules>

    /** Changes the username of the signed in person. The server checks the format, the policy and the wait. */
    suspend fun change(orgId: String, username: String): AdminResult<Unit>

    suspend fun setPolicy(orgId: String, policy: UsernamePolicy): AdminResult<Unit>

    /** An organization admin gives a person a neutral username, for example after a report. */
    suspend fun reset(orgId: String, userId: String): AdminResult<Unit>
}
