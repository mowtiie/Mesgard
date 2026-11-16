package com.mowtiie.mesgard.data.auth

import com.mowtiie.mesgard.domain.model.SignInError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignInErrorsTest {
    @Test
    fun `maps rejected credentials`() {
        assertEquals(SignInError.INVALID_CREDENTIALS, signInErrorFor("invalid_credentials", 400))
    }

    @Test
    fun `maps a disabled login`() {
        assertEquals(SignInError.ACCOUNT_DISABLED, signInErrorFor("user_banned", 400))
    }

    @Test
    fun `maps the rate limit by its code`() {
        assertEquals(SignInError.TOO_MANY_ATTEMPTS, signInErrorFor("over_request_rate_limit", 429))
    }

    @Test
    fun `maps the rate limit by its status when the code is missing`() {
        assertEquals(SignInError.TOO_MANY_ATTEMPTS, signInErrorFor(null, 429))
    }

    @Test
    fun `leaves any other answer without a reason of its own`() {
        assertNull(signInErrorFor("unexpected_failure", 500))
        assertNull(signInErrorFor(null, 400))
    }
}
