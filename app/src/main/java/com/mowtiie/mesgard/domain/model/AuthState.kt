package com.mowtiie.mesgard.domain.model

internal sealed interface AuthState {
    data object Loading : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val userId: String) : AuthState
}
