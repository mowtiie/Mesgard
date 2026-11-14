package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupDetail
import com.mowtiie.mesgard.domain.model.GroupRole

/** Groups and their members, for organization admins. Every call needs a connection to the server. */
internal interface GroupAdminRepository {
    suspend fun listGroups(orgId: String): AdminResult<List<AdminGroup>>

    suspend fun fetchGroup(groupId: String): AdminResult<GroupDetail>

    suspend fun createGroup(orgId: String, name: String): AdminResult<String>

    suspend fun renameGroup(groupId: String, name: String): AdminResult<Unit>

    /** Deletes the group, its roster and its group conversation with the messages in it. */
    suspend fun deleteGroup(groupId: String): AdminResult<Unit>

    suspend fun addMembers(orgId: String, groupId: String, userIds: List<String>): AdminResult<Unit>

    suspend fun removeMember(groupId: String, userId: String): AdminResult<Unit>

    suspend fun changeMemberRole(groupId: String, userId: String, role: GroupRole): AdminResult<Unit>
}
