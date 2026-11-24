package com.mowtiie.mesgard.domain.model

/** Who in an organization may have a profile picture. */
internal enum class ProfilePicturePolicy {
    OFF,
    STAFF_AND_ADMINS,
    EVERYONE,
}

/**
 * What the organization allows and what the signed in person has: [canSet] says whether they may set a picture now,
 * [version] is the picture they have, and [blockedUntilEpochMillis] is when a block after a report ends.
 */
internal data class ProfilePictureRules(
    val orgId: String,
    val userId: String,
    val policy: ProfilePicturePolicy,
    val canSet: Boolean,
    val version: Int?,
    val blockedUntilEpochMillis: Long?,
)
