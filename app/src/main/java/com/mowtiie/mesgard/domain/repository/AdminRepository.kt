package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminResult
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

    suspend fun changeRole(orgId: String, userId: String, role: OrgRole): AdminResult<Unit>
}
