package com.mowtiie.mesgard.domain.model

import com.mowtiie.mesgard.domain.avatar.AvatarRef

internal data class AccountMembership(
    val organizationId: String,
    val organizationName: String,
    val role: OrgRole,
    val avatarVersion: Int? = null,
)

internal data class Account(
    val displayName: String,
    val email: String?,
    val memberships: List<AccountMembership>,
    val userId: String = "",
) {
    /** The picture of the account in the first organization that has one. */
    val avatarRef: AvatarRef?
        get() = memberships.firstNotNullOfOrNull { membership ->
            membership.avatarVersion?.let { AvatarRef(membership.organizationId, userId, it) }
        }
}
