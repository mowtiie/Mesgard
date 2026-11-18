package com.mowtiie.mesgard.ui.server

import android.net.Uri
import androidx.annotation.StringRes
import com.mowtiie.mesgard.R

internal enum class ServerSetupError {
    ADDRESS_EMPTY,
    ADDRESS_INVALID,
    ADDRESS_NOT_SECURE,
    KEY_EMPTY,
    KEY_MALFORMED,
    KEY_SECRET,
    SIGN_OUT_FIRST,
    UNREACHABLE,
    KEY_REJECTED,
    NOT_A_SERVER,
}

internal data class ServerSetupUiState(
    val address: String = "",
    val accessKey: String = "",
    val canCancel: Boolean = false,
    val isChecking: Boolean = false,
    val isFromLink: Boolean = false,
    val hasStartupProblem: Boolean = false,
    val isFinished: Boolean = false,
    val error: ServerSetupError? = null,
) {
    val isAddressError: Boolean
        get() = error in ADDRESS_ERRORS

    val isAccessKeyError: Boolean
        get() = error in ACCESS_KEY_ERRORS

    private companion object {
        val ADDRESS_ERRORS = setOf(
            ServerSetupError.ADDRESS_EMPTY,
            ServerSetupError.ADDRESS_INVALID,
            ServerSetupError.ADDRESS_NOT_SECURE,
            ServerSetupError.UNREACHABLE,
            ServerSetupError.NOT_A_SERVER,
        )
        val ACCESS_KEY_ERRORS = setOf(
            ServerSetupError.KEY_EMPTY,
            ServerSetupError.KEY_MALFORMED,
            ServerSetupError.KEY_SECRET,
            ServerSetupError.KEY_REJECTED,
        )
    }
}

/** The address and key of a setup link such as `mesgard://setup?url=https%3A%2F%2Fchat.school.example&key=...`. */
internal data class ServerSetupPrefill(val address: String, val accessKey: String)

private const val SETUP_SCHEME = "mesgard"
private const val SETUP_HOST = "setup"

internal fun parseSetupLink(uri: Uri?): ServerSetupPrefill? {
    if (uri == null || uri.scheme != SETUP_SCHEME || uri.host != SETUP_HOST) return null
    val address = uri.getQueryParameter("url")
    val accessKey = uri.getQueryParameter("key")
    return if (address.isNullOrBlank() || accessKey.isNullOrBlank()) null else ServerSetupPrefill(address, accessKey)
}

@StringRes
internal fun ServerSetupUiState.helpResource(): Int = when {
    hasStartupProblem -> R.string.server_setup_help_problem
    isFromLink -> R.string.server_setup_link_warning
    else -> R.string.server_setup_help
}

@StringRes
internal fun ServerSetupError.messageResource(): Int = when (this) {
    ServerSetupError.ADDRESS_EMPTY -> R.string.server_setup_error_address_empty
    ServerSetupError.ADDRESS_INVALID -> R.string.server_setup_error_address_invalid
    ServerSetupError.ADDRESS_NOT_SECURE -> R.string.server_setup_error_address_not_secure
    ServerSetupError.KEY_EMPTY -> R.string.server_setup_error_key_empty
    ServerSetupError.KEY_MALFORMED -> R.string.server_setup_error_key_malformed
    ServerSetupError.KEY_SECRET -> R.string.server_setup_error_key_secret
    ServerSetupError.SIGN_OUT_FIRST -> R.string.server_setup_error_sign_out_first
    ServerSetupError.UNREACHABLE -> R.string.server_setup_error_unreachable
    ServerSetupError.KEY_REJECTED -> R.string.server_setup_error_key_rejected
    ServerSetupError.NOT_A_SERVER -> R.string.server_setup_error_not_a_server
}
