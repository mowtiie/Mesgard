package com.mowtiie.mesgard.domain.model

internal enum class OrgRole {
    ORG_ADMIN,
    STAFF,
    STUDENT,
    OTHER,
}

internal data class Contact(val orgId: String, val userId: String, val displayName: String, val role: OrgRole)

internal sealed interface StartConversationResult {
    data class Started(val conversationId: String) : StartConversationResult

    data class Failure(val error: RefreshError) : StartConversationResult
}
