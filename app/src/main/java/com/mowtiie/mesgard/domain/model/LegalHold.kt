package com.mowtiie.mesgard.domain.model

internal const val HOLD_REASON_MIN_LENGTH = 10
internal const val HOLD_RELEASE_NOTE_MIN_LENGTH = 3

internal enum class LegalHoldScope {
    PERSON,
    GROUP,
    ORGANIZATION,
}

/** What a hold protects. A hold on a person or a group names it, and one on the organization names nothing. */
internal sealed interface HoldTarget {
    data class Person(val userId: String, val name: String) : HoldTarget

    data class Group(val groupId: String, val name: String) : HoldTarget

    data object Organization : HoldTarget
}

/** While a hold is active, what the organization keeps about the person or the group is not erased or deleted. */
internal data class LegalHold(
    val id: String,
    val scope: LegalHoldScope,
    val personName: String?,
    val groupName: String?,
    val reason: String,
    val placedByName: String,
    val placedAtEpochMillis: Long,
)
