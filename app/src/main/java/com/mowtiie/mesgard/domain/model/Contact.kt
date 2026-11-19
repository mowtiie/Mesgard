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

/** [Unavailable] means the server could not be asked, so the roster kept on the phone is searched instead. */
internal sealed interface ContactSearchResult {
    data class Found(val contacts: List<Contact>) : ContactSearchResult

    data object Unavailable : ContactSearchResult
}

internal sealed interface StartConversationResult {
    data class Started(val conversationId: String) : StartConversationResult

    data class Failure(val error: RefreshError) : StartConversationResult
}
