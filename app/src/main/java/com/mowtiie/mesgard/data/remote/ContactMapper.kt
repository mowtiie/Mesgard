package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.data.local.ContactEntity
import com.mowtiie.mesgard.domain.model.Contact

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
    role = role.toOrgRole(),
)
