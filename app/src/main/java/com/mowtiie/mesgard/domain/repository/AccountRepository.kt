package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.Account
import com.mowtiie.mesgard.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

internal interface AccountRepository {
    /** The signed in user's account, or null until it has been loaded once. */
    fun observeAccount(): Flow<Account?>

    suspend fun refresh(): RefreshResult
}
