package com.mowtiie.mesgard.domain.model

internal enum class PasswordChangeError {
    WEAK_PASSWORD,
    SAME_PASSWORD,
    NETWORK_UNAVAILABLE,
    UNKNOWN,
}

internal sealed interface PasswordChangeResult {
    data object Success : PasswordChangeResult

    data class Failure(val error: PasswordChangeError) : PasswordChangeResult
}
