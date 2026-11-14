package com.mowtiie.mesgard.ui.server

import android.net.Uri

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
