package com.mowtiie.mesgard.domain.model

internal sealed interface RefreshResult {
    data object Success : RefreshResult

    data class Failure(val error: RefreshError) : RefreshResult
}

internal enum class RefreshError {
    NETWORK_UNAVAILABLE,
    UNKNOWN,
}
