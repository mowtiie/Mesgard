package com.mowtiie.mesgard.domain.model

internal sealed interface AuthState {
    data object Loading : AuthState

    data object SignedOut : AuthState

    /**
     * [mustChangePassword] is set while the user still has the temporary password an admin gave them. [mfaStep] says
     * whether a second factor must be verified or set up first; until it is [MfaStep.NONE] the server shows no data.
     */
    data class SignedIn(
        val userId: String,
        val mustChangePassword: Boolean = false,
        val mfaStep: MfaStep = MfaStep.NONE,
    ) : AuthState
}
