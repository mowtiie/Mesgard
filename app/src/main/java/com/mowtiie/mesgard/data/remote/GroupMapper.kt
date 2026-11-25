package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.data.local.GroupEntity
import com.mowtiie.mesgard.domain.avatar.AvatarRef
import com.mowtiie.mesgard.domain.model.Group

private val GROUP_STARTER_ROLES = setOf("org_admin", "staff")

internal fun GroupDto.toEntity(memberships: List<MembershipDto>) = GroupEntity(
    id = id,
    orgId = orgId,
    name = name,
    canStartConversation = memberships.any { it.orgId == orgId && it.role in GROUP_STARTER_ROLES },
    avatarVersion = avatarVersion,
)

internal fun GroupEntity.toGroup() = Group(
    id = id,
    orgId = orgId,
    name = name,
    canStartConversation = canStartConversation,
    avatar = avatarVersion?.let { AvatarRef(orgId, id, it, isGroup = true) },
)
