package com.mowtiie.mesgard.data.auth

import android.util.Log
import com.mowtiie.mesgard.data.local.LocalDataEraser
import com.mowtiie.mesgard.data.remote.PasswordRemoteSource
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.model.PasswordChangeError
import com.mowtiie.mesgard.domain.model.PasswordChangeResult
import com.mowtiie.mesgard.domain.model.SignInError
import com.mowtiie.mesgard.domain.model.SignInResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "SupabaseAuthRepository"
private const val WEAK_PASSWORD_CODE = "weak_password"
private const val SAME_PASSWORD_CODE = "same_password"
private const val MUST_CHANGE_PASSWORD_KEY = "must_change_password"

internal class SupabaseAuthRepository @Inject constructor(
    private val auth: Auth,
    private val localDataEraser: LocalDataEraser,
    private val passwordSource: PasswordRemoteSource,
    private val mfaStepResolver: MfaStepResolver,
) : AuthRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val authState: Flow<AuthState> = auth.sessionStatus.onEach(::eraseWhenRevoked).mapLatest(::toAuthState)

    override val currentUserId: String?
        get() = auth.currentUserOrNull()?.id

    override val currentUserEmail: String?
        get() = auth.currentUserOrNull()?.email

    override suspend fun signIn(email: String, password: String): SignInResult = try {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        SignInResult.Success
    } catch (exception: RestException) {
        SignInResult.Failure(toSignInError(exception))
    } catch (exception: HttpRequestException) {
        networkFailure(exception)
    } catch (exception: IOException) {
        networkFailure(exception)
    }

    override suspend fun signOut() {
        try {
            auth.signOut()
        } finally {
            localDataEraser.eraseAll()
        }
    }

    override suspend fun changePassword(newPassword: String): PasswordChangeResult = try {
        auth.updateUser { password = newPassword }
        passwordSource.clearChangeRequirement()
        auth.refreshCurrentSession()
        PasswordChangeResult.Success
    } catch (exception: RestException) {
        PasswordChangeResult.Failure(toPasswordChangeError(exception))
    } catch (exception: HttpRequestException) {
        Log.i(TAG, "The password change could not reach the server", exception)
        PasswordChangeResult.Failure(PasswordChangeError.NETWORK_UNAVAILABLE)
    } catch (exception: IOException) {
        Log.i(TAG, "The password change could not reach the server", exception)
        PasswordChangeResult.Failure(PasswordChangeError.NETWORK_UNAVAILABLE)
    }

    /**
     * The end of a session leaves nothing of the account on the phone. The library reports a session the server
     * ended, for example because the login was disabled or the person was removed, the same way as a sign out the
     * person chose, which already erases. Erasing twice changes nothing.
     */
    private suspend fun eraseWhenRevoked(status: SessionStatus) {
        if (status is SessionStatus.NotAuthenticated) localDataEraser.eraseAll()
    }

    private suspend fun toAuthState(status: SessionStatus): AuthState = when (status) {
        SessionStatus.Initializing -> AuthState.Loading
        is SessionStatus.Authenticated -> signedInOrSignedOut(status.session.user)
        is SessionStatus.RefreshFailure -> signedInOrSignedOut(auth.currentSessionOrNull()?.user)
        is SessionStatus.NotAuthenticated -> AuthState.SignedOut
    }

    private suspend fun signedInOrSignedOut(user: UserInfo?): AuthState = if (user == null) {
        AuthState.SignedOut
    } else {
        AuthState.SignedIn(user.id, user.appMetadata.requiresPasswordChange(), mfaStepResolver.resolve(user))
    }

    private fun toPasswordChangeError(exception: RestException): PasswordChangeError = when (exception.error) {
        WEAK_PASSWORD_CODE -> PasswordChangeError.WEAK_PASSWORD
        SAME_PASSWORD_CODE -> PasswordChangeError.SAME_PASSWORD
        else -> {
            Log.w(TAG, "The password change was rejected: ${exception.error}", exception)
            PasswordChangeError.UNKNOWN
        }
    }

    private fun toSignInError(exception: RestException): SignInError =
        signInErrorFor(exception.error, exception.statusCode) ?: run {
            Log.w(TAG, "Sign in rejected by the server: ${exception.error}", exception)
            SignInError.UNKNOWN
        }

    private fun networkFailure(cause: Exception): SignInResult {
        Log.i(TAG, "Sign in could not reach the server", cause)
        return SignInResult.Failure(SignInError.NETWORK_UNAVAILABLE)
    }
}

/** App metadata can only be written on the server, so an admin's mark cannot be removed or forged by the app. */
internal fun JsonObject?.requiresPasswordChange(): Boolean =
    this?.get(MUST_CHANGE_PASSWORD_KEY)?.jsonPrimitive?.booleanOrNull == true
