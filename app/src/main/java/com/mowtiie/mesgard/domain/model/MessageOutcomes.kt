package com.mowtiie.mesgard.domain.model

internal sealed interface PageResult {
    data class Loaded(val endReached: Boolean) : PageResult

    data class Failure(val error: RefreshError) : PageResult
}

internal sealed interface SendResult {
    data object Queued : SendResult

    data class Rejected(val reason: SendRejection) : SendResult
}

internal enum class SendRejection {
    EMPTY_MESSAGE,
    MESSAGE_TOO_LONG,
    NOT_SIGNED_IN,
    CONVERSATION_UNAVAILABLE,
}
