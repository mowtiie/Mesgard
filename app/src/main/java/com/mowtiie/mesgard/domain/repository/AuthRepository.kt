package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.model.SignInResult
import kotlinx.coroutines.flow.Flow

internal interface AuthRepository {
    val authState: Flow<AuthState>

    val currentUserId: String?

    val currentUserEmail: String?

    suspend fun signIn(email: String, password: String): SignInResult

    suspend fun signOut()
}
