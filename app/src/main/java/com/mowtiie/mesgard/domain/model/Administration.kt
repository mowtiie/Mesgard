package com.mowtiie.mesgard.domain.model

internal data class NewMember(val email: String, val displayName: String, val role: OrgRole)

internal enum class SkipReason {
    EMAIL_TAKEN,
    DUPLICATE,
    INVALID_EMAIL,
    INVALID_ROLE,
    INVALID_NAME,
    FAILED,
}

/** What happened to one person of a batch. A created login comes with the temporary password, shown once. */
internal sealed interface MemberCreation {
    val email: String

    data class Created(override val email: String, val userId: String, val temporaryPassword: String) : MemberCreation

    data class Skipped(override val email: String, val reason: SkipReason) : MemberCreation
}

internal data class RemovedMember(val loginDisabled: Boolean)

internal enum class AdminError {
    NETWORK_UNAVAILABLE,
    NOT_ALLOWED,
    NOT_A_MEMBER,
    LAST_ADMIN,
    UNKNOWN,
}

internal sealed interface AdminResult<out T> {
    data class Success<T>(val value: T) : AdminResult<T>

    data class Failure(val error: AdminError) : AdminResult<Nothing>
}

internal enum class GroupRole {
    MEMBER,
    MODERATOR,
}

internal data class AdminGroup(val id: String, val name: String, val memberCount: Int)

internal data class GroupMember(val userId: String, val role: GroupRole)

internal data class GroupDetail(val id: String, val name: String, val members: List<GroupMember>)
