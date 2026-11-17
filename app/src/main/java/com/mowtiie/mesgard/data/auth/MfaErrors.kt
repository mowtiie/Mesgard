package com.mowtiie.mesgard.data.auth

import com.mowtiie.mesgard.domain.model.MfaError

private const val VERIFICATION_FAILED_CODE = "mfa_verification_failed"
private const val RATE_LIMIT_CODE = "over_request_rate_limit"
private const val HTTP_TOO_MANY_REQUESTS = 429

/** Maps what the Auth server answered to the reason shown on the code screen. */
internal fun mfaErrorFor(code: String?, statusCode: Int): MfaError = when {
    code == VERIFICATION_FAILED_CODE -> MfaError.INVALID_CODE
    code == RATE_LIMIT_CODE || statusCode == HTTP_TOO_MANY_REQUESTS -> MfaError.TOO_MANY_ATTEMPTS
    else -> MfaError.UNKNOWN
}
