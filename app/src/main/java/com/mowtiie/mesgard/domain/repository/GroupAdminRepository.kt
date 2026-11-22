package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupDetail
import com.mowtiie.mesgard.domain.model.GroupPlacement
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.model.GroupSettings

/** Groups and their members, for organization admins. Every call needs a connection to the server. */
internal interface GroupAdminRepository {
    suspend fun listGroups(orgId: String): AdminResult<List<AdminGroup>>

    suspend fun fetchGroup(groupId: String): AdminResult<GroupDetail>

    suspend fun createGroup(
        orgId: String,
        name: String,
        settings: GroupSettings = GroupSettings(),
        placement: GroupPlacement = GroupPlacement(),
    ): AdminResult<String>

    suspend fun changeSettings(groupId: String, settings: GroupSettings): AdminResult<Unit>

    suspend fun renameGroup(groupId: String, name: String): AdminResult<Unit>

    /** Deletes the group, its roster and its group conversation with the messages in it. */
    suspend fun deleteGroup(groupId: String): AdminResult<Unit>

    suspend fun addMembers(orgId: String, groupId: String, userIds: List<String>): AdminResult<Unit>

    suspend fun removeMember(groupId: String, userId: String): AdminResult<Unit>

    suspend fun changeMemberRole(groupId: String, userId: String, role: GroupRole): AdminResult<Unit>
}
