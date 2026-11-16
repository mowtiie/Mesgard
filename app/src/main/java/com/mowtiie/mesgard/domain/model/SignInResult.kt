package com.mowtiie.mesgard.domain.model

internal sealed interface SignInResult {
    data object Success : SignInResult

    data class Failure(val error: SignInError) : SignInResult
}

internal enum class SignInError {
    INVALID_CREDENTIALS,
    ACCOUNT_DISABLED,
    TOO_MANY_ATTEMPTS,
    NETWORK_UNAVAILABLE,
    UNKNOWN,
}
