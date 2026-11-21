package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.data.remote.OnlineStatusRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toRefreshResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.OnlineStatusRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "OnlineStatus"

@Singleton
internal class RemoteOnlineStatusRepository @Inject constructor(
    private val remote: OnlineStatusRemoteSource,
    private val authRepository: AuthRepository,
) : OnlineStatusRepository {
    private val mutableChoice = MutableStateFlow<Boolean?>(null)
    override val choice: StateFlow<Boolean?> = mutableChoice.asStateFlow()

    override suspend fun refresh(): RefreshResult {
        val userId = authRepository.currentUserId ?: return RefreshResult.Failure(RefreshError.UNKNOWN)
        return attemptRemote(TAG) { mutableChoice.value = remote.fetchChoice(userId) }.toRefreshResult()
    }

    override suspend fun setChoice(show: Boolean): RefreshResult {
        val previous = mutableChoice.value
        mutableChoice.value = show
        val outcome = attemptRemote(TAG) { remote.saveChoice(show) }
        if (outcome is RemoteOutcome.Failure) mutableChoice.value = previous
        return outcome.toRefreshResult()
    }

    override fun clear() {
        mutableChoice.value = null
    }
}
