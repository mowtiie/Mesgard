package com.mowtiie.mesgard.data.auth

import android.util.Log
import com.mowtiie.mesgard.domain.model.MfaEnrollment
import com.mowtiie.mesgard.domain.model.MfaError
import com.mowtiie.mesgard.domain.model.MfaResult
import com.mowtiie.mesgard.domain.repository.MfaRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val TAG = "SupabaseMfaRepository"
private const val ISSUER = "Mesgard"
private const val FRIENDLY_NAME_SUFFIX_LENGTH = 8

internal class SupabaseMfaRepository @Inject constructor(private val auth: Auth) : MfaRepository {
    override val isEnabled: Flow<Boolean> =
        auth.sessionStatus.map { auth.mfa.verifiedFactors.isNotEmpty() }.distinctUntilChanged()

    override suspend fun startEnrollment(): MfaResult<MfaEnrollment> = attempt {
        auth.retrieveUserForCurrentSession().factors.filterNot { it.isVerified }.forEach { auth.mfa.unenroll(it.id) }
        val factor = auth.mfa.enroll(FactorType.TOTP, uniqueFriendlyName()) { issuer = ISSUER }
        MfaEnrollment(factor.id, factor.data.secret, factor.data.uri)
    }

    override suspend fun confirmEnrollment(enrollment: MfaEnrollment, code: String): MfaResult<Unit> = attempt<Unit> {
        auth.mfa.createChallengeAndVerify(enrollment.factorId, code)
    }

    override suspend fun verifySignIn(code: String): MfaResult<Unit> = attempt<Unit> {
        val factor = auth.retrieveUserForCurrentSession().factors.first { it.isVerified }
        auth.mfa.createChallengeAndVerify(factor.id, code)
    }

    override suspend fun disable(): MfaResult<Unit> = attempt<Unit> {
        auth.retrieveUserForCurrentSession().factors.forEach { auth.mfa.unenroll(it.id) }
        auth.retrieveUserForCurrentSession(updateSession = true)
    }

    private suspend fun <T> attempt(block: suspend () -> T): MfaResult<T> = try {
        MfaResult.Success(block())
    } catch (exception: RestException) {
        failure(mfaErrorFor(exception.error, exception.statusCode), exception)
    } catch (exception: HttpRequestException) {
        failure(MfaError.NETWORK_UNAVAILABLE, exception)
    } catch (exception: IOException) {
        failure(MfaError.NETWORK_UNAVAILABLE, exception)
    } catch (exception: NoSuchElementException) {
        failure(MfaError.UNKNOWN, exception)
    }

    // The message of an exception can carry the code, so only its type is logged.
    private fun failure(error: MfaError, exception: Exception): MfaResult.Failure {
        Log.w(TAG, "Two-step verification failed: $error (${exception.javaClass.simpleName})")
        return MfaResult.Failure(error)
    }

    private fun uniqueFriendlyName(): String =
        "$ISSUER ${UUID.randomUUID().toString().take(FRIENDLY_NAME_SUFFIX_LENGTH)}"
}
