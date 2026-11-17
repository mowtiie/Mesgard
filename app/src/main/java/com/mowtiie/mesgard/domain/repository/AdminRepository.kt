package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.ErasureMode
import com.mowtiie.mesgard.domain.model.MemberCreation
import com.mowtiie.mesgard.domain.model.NewMember
import com.mowtiie.mesgard.domain.model.OrgRole
import com.mowtiie.mesgard.domain.model.RemovedMember

/** Roster administration for organization admins. Every call needs a connection to the server. */
internal interface AdminRepository {
    suspend fun createMembers(orgId: String, members: List<NewMember>): AdminResult<List<MemberCreation>>

    /** Returns the new temporary password, which the person must replace at the next sign in. */
    suspend fun resetPassword(orgId: String, userId: String): AdminResult<String>

    suspend fun removeMember(orgId: String, userId: String): AdminResult<RemovedMember>

    /** Removes every second factor of the person, so they set one up again at the next sign in. */
    suspend fun resetMfa(orgId: String, userId: String): AdminResult<Unit>

    /** What the server does when someone is erased. A server that cannot say is treated as not offering it. */
    suspend fun erasureMode(): AdminResult<ErasureMode>

    /** Removes the person and erases their personal data. It cannot be undone. */
    suspend fun eraseMember(orgId: String, userId: String): AdminResult<Unit>

    suspend fun changeRole(orgId: String, userId: String, role: OrgRole): AdminResult<Unit>
}
