package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.GroupAdminRemoteSource
import com.mowtiie.mesgard.domain.model.AdminGroup
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.GroupDetail
import com.mowtiie.mesgard.domain.model.GroupMember
import com.mowtiie.mesgard.domain.model.GroupRole
import com.mowtiie.mesgard.domain.repository.GroupAdminRepository
import javax.inject.Inject

private const val MODERATOR = "moderator"
private const val MEMBER = "member"

/** Groups are never cached: administration only works with a connection, and every answer comes from the server. */
internal class RemoteGroupAdminRepository @Inject constructor(private val remote: GroupAdminRemoteSource) :
    GroupAdminRepository {
    override suspend fun listGroups(orgId: String): AdminResult<List<AdminGroup>> = attemptAdmin {
        remote.listGroups(orgId).map { AdminGroup(it.id, it.name, it.members.firstOrNull()?.count ?: 0) }
    }

    override suspend fun fetchGroup(groupId: String): AdminResult<GroupDetail> = attemptAdmin {
        val name = checkNotNull(remote.fetchGroupName(groupId)) { "The group is not visible" }
        GroupDetail(groupId, name, remote.fetchGroupMembers(groupId).map { GroupMember(it.userId, it.role.toRole()) })
    }

    override suspend fun createGroup(orgId: String, name: String): AdminResult<String> =
        attemptAdmin { remote.createGroup(orgId, name.trim()) }

    override suspend fun renameGroup(groupId: String, name: String): AdminResult<Unit> =
        attemptAdmin { check(remote.renameGroup(groupId, name.trim())) { "No group was changed" } }

    override suspend fun deleteGroup(groupId: String): AdminResult<Unit> =
        attemptAdmin { check(remote.deleteGroup(groupId)) { "No group was deleted" } }

    override suspend fun addMembers(orgId: String, groupId: String, userIds: List<String>): AdminResult<Unit> =
        attemptAdmin { remote.addMembers(orgId, groupId, userIds) }

    override suspend fun removeMember(groupId: String, userId: String): AdminResult<Unit> =
        attemptAdmin { check(remote.removeMember(groupId, userId)) { "No member was removed" } }

    override suspend fun changeMemberRole(groupId: String, userId: String, role: GroupRole): AdminResult<Unit> =
        attemptAdmin { check(remote.changeMemberRole(groupId, userId, role.toWireName())) { "No member was changed" } }
}

private fun String.toRole() = if (this == MODERATOR) GroupRole.MODERATOR else GroupRole.MEMBER

private fun GroupRole.toWireName() = if (this == GroupRole.MODERATOR) MODERATOR else MEMBER
