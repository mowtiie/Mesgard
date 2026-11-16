package com.mowtiie.mesgard.data.auth

import com.mowtiie.mesgard.domain.model.SignInError

private const val INVALID_CREDENTIALS_CODE = "invalid_credentials"
private const val USER_BANNED_CODE = "user_banned"
private const val RATE_LIMIT_CODE = "over_request_rate_limit"
private const val HTTP_TOO_MANY_REQUESTS = 429

/**
 * Maps what the Auth server answered to the reason shown on the Sign in screen, or null for an answer that has no
 * message of its own. A disabled login is what removing a member from their last organization leaves behind.
 */
internal fun signInErrorFor(code: String?, statusCode: Int): SignInError? = when {
    code == INVALID_CREDENTIALS_CODE -> SignInError.INVALID_CREDENTIALS
    code == USER_BANNED_CODE -> SignInError.ACCOUNT_DISABLED
    code == RATE_LIMIT_CODE || statusCode == HTTP_TOO_MANY_REQUESTS -> SignInError.TOO_MANY_ATTEMPTS
    else -> null
}
