package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.model.SignInResult
import kotlinx.coroutines.flow.Flow

internal interface AuthRepository {
    val authState: Flow<AuthState>

    suspend fun signIn(email: String, password: String): SignInResult

    suspend fun signOut()
}
