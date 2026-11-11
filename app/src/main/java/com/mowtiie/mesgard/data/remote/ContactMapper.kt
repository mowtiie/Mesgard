package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.data.local.ContactEntity
import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.OrgRole

private const val ADMIN_ROLE = "org_admin"
private const val STAFF_ROLE = "staff"
private const val STUDENT_ROLE = "student"

internal fun ContactDto.toEntity() = ContactEntity(
    orgId = orgId,
    userId = userId,
    displayName = user.displayName,
    role = role,
)

internal fun ContactEntity.toContact() = Contact(
    orgId = orgId,
    userId = userId,
    displayName = displayName,
    role = when (role) {
        ADMIN_ROLE -> OrgRole.ORG_ADMIN
        STAFF_ROLE -> OrgRole.STAFF
        STUDENT_ROLE -> OrgRole.STUDENT
        else -> OrgRole.OTHER
    },
)
