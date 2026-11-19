package com.mowtiie.mesgard.domain.model

internal enum class OrgRole {
    ORG_ADMIN,
    STAFF,
    MEMBER,
    OTHER,
}

internal data class Contact(
    val orgId: String,
    val userId: String,
    val displayName: String,
    val role: OrgRole,
    val isFavorite: Boolean = false,
)

internal sealed interface StartConversationResult {
    data class Started(val conversationId: String) : StartConversationResult

    data class Failure(val error: RefreshError) : StartConversationResult
}
