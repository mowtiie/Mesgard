package com.mowtiie.mesgard.domain.model

internal sealed interface AuthState {
    data object Loading : AuthState

    data object SignedOut : AuthState

    /** [mustChangePassword] is set while the user still has the temporary password an admin gave them. */
    data class SignedIn(val userId: String, val mustChangePassword: Boolean = false) : AuthState
}
