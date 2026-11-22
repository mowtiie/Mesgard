package com.mowtiie.mesgard.domain.model

/** Who in an organization may create groups. Organization admins always may. */
internal enum class GroupCreationPolicy {
    ADMINS_ONLY,
    STAFF,
    EVERYONE,
}

/**
 * What the organization allows and what the signed in person may do: [canCreate] says whether they may create a group,
 * [created] how many they made already and [limit] how many a person who is not an admin may make.
 */
internal data class GroupCreationRules(
    val policy: GroupCreationPolicy,
    val canCreate: Boolean,
    val created: Int,
    val limit: Int,
)
