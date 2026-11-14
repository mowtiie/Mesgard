package com.mowtiie.mesgard.domain.auth

internal const val MIN_PASSWORD_LENGTH = 8

internal enum class PasswordProblem {
    TOO_SHORT,
    MISMATCH,
}

/** The checks the app makes before asking the server, which applies its own rules as well. */
internal fun findPasswordProblem(password: String, confirmation: String): PasswordProblem? = when {
    password.length < MIN_PASSWORD_LENGTH -> PasswordProblem.TOO_SHORT
    password != confirmation -> PasswordProblem.MISMATCH
    else -> null
}
