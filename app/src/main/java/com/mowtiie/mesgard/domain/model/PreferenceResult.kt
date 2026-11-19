package com.mowtiie.mesgard.domain.model

internal const val MAX_PINNED_CONVERSATIONS = 3

internal sealed interface PreferenceResult {
    data object Success : PreferenceResult

    data class Failure(val error: PreferenceError) : PreferenceResult
}

internal enum class PreferenceError {
    NETWORK_UNAVAILABLE,
    TOO_MANY_PINNED,
    UNKNOWN,
}
