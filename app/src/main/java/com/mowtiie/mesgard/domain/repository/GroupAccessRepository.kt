package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AccessedMessage
import com.mowtiie.mesgard.domain.model.AdminResult

/**
 * Break-glass access to the messages of a group a person created. An organization admin opens them with a reason for a
 * week and can only read. Every opening is in the audit log and the members of the group can see when it happened. It
 * needs a connection and nothing is kept on the phone.
 */
internal interface GroupAccessRepository {
    suspend fun open(groupId: String, reason: String): AdminResult<Unit>

    /** The messages newest first, the page after [before] when given. */
    suspend fun messages(groupId: String, before: AccessedMessage?, pageSize: Int): AdminResult<List<AccessedMessage>>

    /** When an administrator opened the group lately, newest first, for the members of the group to see. */
    suspend fun openings(groupId: String): AdminResult<List<Long>>
}
