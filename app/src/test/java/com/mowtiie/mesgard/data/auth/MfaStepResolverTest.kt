package com.mowtiie.mesgard.data.auth

import android.util.Log
import com.mowtiie.mesgard.data.remote.MfaRemoteSource
import com.mowtiie.mesgard.domain.model.MfaError
import com.mowtiie.mesgard.domain.model.MfaStep
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.mfa.AuthenticatorAssuranceLevel
import io.github.jan.supabase.auth.mfa.MfaLevel
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserMfaFactor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MfaStepResolverTest {
    private val remoteSource = mockk<MfaRemoteSource>()
    private val auth = mockk<Auth>()

    @Before
    fun silenceLogging() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun restoreLogging() {
        unmockkStatic(Log::class)
    }

    private fun resolver() = MfaStepResolver(auth, remoteSource)

    private fun sessionAt(level: AuthenticatorAssuranceLevel) {
        every { auth.mfa.getAuthenticatorAssuranceLevel() } returns MfaLevel(level, level, emptyList())
    }

    private fun user(verifiedFactor: Boolean): UserInfo {
        val factor = mockk<UserMfaFactor> { every { isVerified } returns verifiedFactor }
        return mockk { every { factors } returns listOf(factor) }
    }

    @Test
    fun `needs nothing once the session is aal2`() = runTest {
        sessionAt(AuthenticatorAssuranceLevel.AAL2)

        assertEquals(MfaStep.NONE, resolver().resolve(user(verifiedFactor = true)))
        coVerify(exactly = 0) { remoteSource.isRequired() }
    }

    @Test
    fun `asks for the code when an aal1 session has a verified factor`() = runTest {
        sessionAt(AuthenticatorAssuranceLevel.AAL1)

        assertEquals(MfaStep.VERIFY, resolver().resolve(user(verifiedFactor = true)))
        coVerify(exactly = 0) { remoteSource.isRequired() }
    }

    @Test
    fun `ignores a factor that was never confirmed`() = runTest {
        sessionAt(AuthenticatorAssuranceLevel.AAL1)
        coEvery { remoteSource.isRequired() } returns false

        assertEquals(MfaStep.NONE, resolver().resolve(user(verifiedFactor = false)))
    }

    @Test
    fun `asks to set up a factor when the server requires one`() = runTest {
        sessionAt(AuthenticatorAssuranceLevel.AAL1)
        coEvery { remoteSource.isRequired() } returns true

        assertEquals(MfaStep.ENROLL, resolver().resolve(user(verifiedFactor = false)))
    }

    @Test
    fun `does not trap anyone when the server cannot be reached`() = runTest {
        sessionAt(AuthenticatorAssuranceLevel.AAL1)
        coEvery { remoteSource.isRequired() } throws IOException("offline")

        assertEquals(MfaStep.NONE, resolver().resolve(user(verifiedFactor = false)))
    }

    @Test
    fun `treats a session without an assurance level as not verified`() = runTest {
        every { auth.mfa.getAuthenticatorAssuranceLevel() } throws IllegalStateException("No 'aal' claim found in JWT")
        coEvery { remoteSource.isRequired() } returns true

        assertEquals(MfaStep.ENROLL, resolver().resolve(user(verifiedFactor = false)))
    }

    @Test
    fun `maps what the Auth server answers to a reason`() {
        assertEquals(MfaError.INVALID_CODE, mfaErrorFor("mfa_verification_failed", 422))
        assertEquals(MfaError.TOO_MANY_ATTEMPTS, mfaErrorFor("over_request_rate_limit", 429))
        assertEquals(MfaError.TOO_MANY_ATTEMPTS, mfaErrorFor(null, 429))
        assertEquals(MfaError.UNKNOWN, mfaErrorFor("unexpected_failure", 500))
    }
}
