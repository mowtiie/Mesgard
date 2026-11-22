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
    NOT_ERASABLE,
    LEGAL_HOLD,
    GROUP_NOT_EMPTY,
    GROUP_TREE_INVALID,
    GROUP_LIMIT,
    UNKNOWN,
}

/** What this server does when someone is erased. OFF hides the choice altogether. */
internal enum class ErasureMode {
    OFF,
    KEEP_MESSAGES,
    BLANK_MESSAGES,
}

internal sealed interface AdminResult<out T> {
    data class Success<T>(val value: T) : AdminResult<T>

    data class Failure(val error: AdminError) : AdminResult<Nothing>
}

internal enum class GroupRole {
    OWNER,
    MODERATOR,
    MEMBER,
}

/** What a group is in the organization, so people find it where they expect it. */
internal enum class GroupKind {
    SECTION,
    CLASS,
    DEPARTMENT,
    CLUB,
    OTHER,
}

/** Where a new group sits: its kind and the group it is inside, or none for a group at the top. */
internal data class GroupPlacement(val kind: GroupKind = GroupKind.OTHER, val parentId: String? = null)

/** The tree is at most this many levels deep. The server checks it too, this only keeps the picker honest. */
internal const val MAX_GROUP_DEPTH = 5

internal data class AdminGroup(
    val id: String,
    val name: String,
    val memberCount: Int,
    val kind: GroupKind = GroupKind.OTHER,
    val parentId: String? = null,
)

internal data class GroupMember(val userId: String, val role: GroupRole)

/** Who can post in a group chat. Organization admins can always post. */
internal enum class PostingPolicy {
    EVERYONE,
    MODERATORS,
}

/** [includesEveryone] puts every member of the organization in the group, now and when they join. */
internal data class GroupSettings(
    val postingPolicy: PostingPolicy = PostingPolicy.EVERYONE,
    val includesEveryone: Boolean = false,
)

internal data class GroupDetail(
    val id: String,
    val name: String,
    val members: List<GroupMember>,
    val settings: GroupSettings = GroupSettings(),
    val placement: GroupPlacement = GroupPlacement(),
    /** Created by a person, not an administrator: its messages need break-glass access to be read. */
    val isUserCreated: Boolean = false,
)
