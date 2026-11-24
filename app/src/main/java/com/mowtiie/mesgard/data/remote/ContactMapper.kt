package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.data.local.ContactEntity
import com.mowtiie.mesgard.domain.model.Contact

internal fun ContactDto.toEntity(isFavorite: Boolean = false) = ContactEntity(
    orgId = orgId,
    userId = userId,
    displayName = user.displayName,
    role = role,
    isFavorite = isFavorite,
    username = username,
    avatarVersion = avatarVersion,
)

internal fun Contact.toStoredEntity() = ContactEntity(
    orgId = orgId,
    userId = userId,
    displayName = displayName,
    role = role.name.lowercase(),
    isFavorite = isFavorite,
    username = username,
    avatarVersion = avatarVersion,
)

internal fun ContactEntity.toContact() = Contact(
    orgId = orgId,
    userId = userId,
    displayName = displayName,
    role = role.toOrgRole(),
    isFavorite = isFavorite,
    username = username,
    avatarVersion = avatarVersion,
)
