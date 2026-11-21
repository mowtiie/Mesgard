package com.mowtiie.mesgard.data.sync

import android.util.Log
import com.mowtiie.mesgard.data.remote.PresenceMembership
import com.mowtiie.mesgard.data.remote.PresenceRemoteSource
import com.mowtiie.mesgard.domain.lifecycle.AppVisibility
import com.mowtiie.mesgard.domain.presence.PresenceTracker
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.OnlineStatusRepository
import com.mowtiie.mesgard.domain.repository.onlineStatusDefault
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

private const val TAG = "PresenceTracker"
private const val RETRY_BASE_DELAY_MS = 1_000L
private const val RETRY_MAX_DELAY_MS = 30_000L
private const val MAX_BACKOFF_EXPONENT = 5

@Singleton
internal class RealtimePresenceTracker @Inject constructor(
    private val source: PresenceRemoteSource,
    private val authRepository: AuthRepository,
    private val appVisibility: AppVisibility,
    private val onlineStatus: OnlineStatusRepository,
) : PresenceTracker {
    private val mutableOnlineUserIds = MutableStateFlow(emptySet<String>())
    private var job: Job? = null

    override val onlineUserIds: StateFlow<Set<String>> = mutableOnlineUserIds.asStateFlow()

    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            appVisibility.isForeground.collectLatest { visible ->
                if (visible) trackPresence() else mutableOnlineUserIds.value = emptySet()
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        mutableOnlineUserIds.value = emptySet()
        onlineStatus.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun trackPresence() {
        val userId = authRepository.currentUserId ?: return
        onlineStatus.refresh()
        combine(flow { emit(source.fetchMemberships(userId)) }, onlineStatus.choice, ::Pair)
            .flatMapLatest { (memberships, choice) -> onlineIn(memberships, userId, choice) }
            .retryWhen { cause, attempt ->
                Log.w(TAG, "The presence subscription failed, retrying", cause)
                delay(min(RETRY_BASE_DELAY_MS shl min(attempt.toInt(), MAX_BACKOFF_EXPONENT), RETRY_MAX_DELAY_MS))
                true
            }
            .collect { mutableOnlineUserIds.value = it }
    }

    /** A person who has not chosen is shown unless they are a student there. A choice applies everywhere. */
    private fun onlineIn(memberships: List<PresenceMembership>, userId: String, choice: Boolean?): Flow<Set<String>> {
        if (memberships.isEmpty()) return emptyFlow()
        val sessions = memberships.map {
            source.session(it.organizationId, userId, announce = choice ?: onlineStatusDefault(it.isStudent))
        }
        return combine(sessions) { perOrganization ->
            perOrganization.fold(emptySet()) { all, online -> all + online }
        }
    }
}
