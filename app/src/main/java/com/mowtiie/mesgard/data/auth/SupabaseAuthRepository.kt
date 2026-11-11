package com.mowtiie.mesgard.data.auth

import android.util.Log
import com.mowtiie.mesgard.data.local.LocalDataEraser
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.model.SignInError
import com.mowtiie.mesgard.domain.model.SignInResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "SupabaseAuthRepository"
private const val INVALID_CREDENTIALS_CODE = "invalid_credentials"

internal class SupabaseAuthRepository @Inject constructor(
    private val auth: Auth,
    private val localDataEraser: LocalDataEraser,
) : AuthRepository {
    override val authState: Flow<AuthState> = auth.sessionStatus.map(::toAuthState)

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

    private fun toAuthState(status: SessionStatus): AuthState = when (status) {
        SessionStatus.Initializing -> AuthState.Loading
        is SessionStatus.Authenticated -> signedInOrSignedOut(status.session.user?.id)
        is SessionStatus.RefreshFailure -> signedInOrSignedOut(auth.currentSessionOrNull()?.user?.id)
        is SessionStatus.NotAuthenticated -> AuthState.SignedOut
    }

    private fun signedInOrSignedOut(userId: String?): AuthState =
        if (userId == null) AuthState.SignedOut else AuthState.SignedIn(userId)

    private fun toSignInError(exception: RestException): SignInError {
        if (exception.error == INVALID_CREDENTIALS_CODE) return SignInError.INVALID_CREDENTIALS
        Log.w(TAG, "Sign in rejected by the server: ${exception.error}", exception)
        return SignInError.UNKNOWN
    }

    private fun networkFailure(cause: Exception): SignInResult {
        Log.i(TAG, "Sign in could not reach the server", cause)
        return SignInResult.Failure(SignInError.NETWORK_UNAVAILABLE)
    }
}
