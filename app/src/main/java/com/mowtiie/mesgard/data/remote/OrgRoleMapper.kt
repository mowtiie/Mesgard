package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.model.OrgRole

private const val ADMIN_ROLE = "org_admin"
private const val STAFF_ROLE = "staff"
private const val MEMBER_ROLE = "member"
private const val LEGACY_MEMBER_ROLE = "student"

internal fun String.toOrgRole(): OrgRole = when (this) {
    ADMIN_ROLE -> OrgRole.ORG_ADMIN
    STAFF_ROLE -> OrgRole.STAFF
    MEMBER_ROLE, LEGACY_MEMBER_ROLE -> OrgRole.MEMBER
    else -> OrgRole.OTHER
}
