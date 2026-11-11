package com.mowtiie.mesgard.ui.auth

import com.mowtiie.mesgard.domain.model.SignInError

internal data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: SignInError? = null,
    val serverHost: String = "",
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotEmpty() && !isSubmitting
}
