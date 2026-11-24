package com.mowtiie.mesgard.domain.model

internal const val USERNAME_MIN_LENGTH = 3
internal const val USERNAME_MAX_LENGTH = 20
internal const val USERNAME_CHANGE_DAYS = 30

/** Who in an organization may change their own username. Organization admins can always set any. */
internal enum class UsernamePolicy {
    EVERYONE,
    STAFF_AND_ADMINS,
    NOBODY,
}

/**
 * The username of the signed in person in one organization. [policyAllowsChange] is what the organization allows and
 * [nextChangeAtEpochMillis] is when the 30 day wait ends, null when it is over.
 */
internal data class UsernameRules(
    val orgId: String,
    val policy: UsernamePolicy,
    val username: String,
    val policyAllowsChange: Boolean,
    val nextChangeAtEpochMillis: Long?,
)
