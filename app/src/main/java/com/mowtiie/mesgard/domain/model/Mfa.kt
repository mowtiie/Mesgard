package com.mowtiie.mesgard.domain.model

/** What a signed in person still has to do with a second factor before the app opens. */
internal enum class MfaStep {
    NONE,
    VERIFY,
    ENROLL,
}

/** The secret is shown once and never stored or logged. [uri] is the otpauth link an authenticator app opens. */
internal data class MfaEnrollment(val factorId: String, val secret: String, val uri: String) {
    override fun toString(): String = "MfaEnrollment(factorId=$factorId)"
}

internal enum class MfaError {
    INVALID_CODE,
    TOO_MANY_ATTEMPTS,
    NETWORK_UNAVAILABLE,
    UNKNOWN,
}

internal sealed interface MfaResult<out T> {
    data class Success<T>(val value: T) : MfaResult<T>

    data class Failure(val error: MfaError) : MfaResult<Nothing>
}
