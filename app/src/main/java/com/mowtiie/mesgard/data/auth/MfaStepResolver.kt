package com.mowtiie.mesgard.data.auth

import android.util.Log
import com.mowtiie.mesgard.data.remote.MfaRemoteSource
import com.mowtiie.mesgard.domain.model.MfaStep
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.mfa.AuthenticatorAssuranceLevel
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "MfaStepResolver"

internal class MfaStepResolver @Inject constructor(private val auth: Auth, private val remoteSource: MfaRemoteSource) {
    suspend fun resolve(user: UserInfo): MfaStep = mfaStepFor(
        isVerified = currentLevel() == AuthenticatorAssuranceLevel.AAL2,
        hasVerifiedFactor = user.factors.any { it.isVerified },
        isRequired = ::isRequired,
    )

    private fun currentLevel(): AuthenticatorAssuranceLevel? = try {
        auth.mfa.getAuthenticatorAssuranceLevel().current
    } catch (exception: IllegalStateException) {
        Log.w(TAG, "The session has no assurance level", exception)
        null
    }

    /**
     * When the server cannot be asked the answer is no, so a flaky connection does not trap anyone on a screen they may
     * not need. The server still refuses data to an account that needs the second factor.
     */
    private suspend fun isRequired(): Boolean = try {
        remoteSource.isRequired()
    } catch (exception: RestException) {
        Log.w(TAG, "The two-step verification status was rejected", exception)
        false
    } catch (exception: HttpRequestException) {
        Log.i(TAG, "The two-step verification status could not be read", exception)
        false
    } catch (exception: IOException) {
        Log.i(TAG, "The two-step verification status could not be read", exception)
        false
    }
}

/** The order matters: a verified session needs nothing, a factor must be verified, otherwise the server decides. */
internal suspend fun mfaStepFor(
    isVerified: Boolean,
    hasVerifiedFactor: Boolean,
    isRequired: suspend () -> Boolean,
): MfaStep = when {
    isVerified -> MfaStep.NONE
    hasVerifiedFactor -> MfaStep.VERIFY
    isRequired() -> MfaStep.ENROLL
    else -> MfaStep.NONE
}
