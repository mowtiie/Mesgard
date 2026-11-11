package com.mowtiie.mesgard.data.sync

import android.util.Log
import com.mowtiie.mesgard.data.remote.PresenceRemoteSource
import com.mowtiie.mesgard.domain.presence.PresenceTracker
import com.mowtiie.mesgard.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
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
) : PresenceTracker {
    private val isForeground = MutableStateFlow(false)
    private val mutableOnlineUserIds = MutableStateFlow(emptySet<String>())
    private var job: Job? = null

    override val onlineUserIds: StateFlow<Set<String>> = mutableOnlineUserIds.asStateFlow()

    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            isForeground.collectLatest { visible ->
                if (visible) trackPresence() else mutableOnlineUserIds.value = emptySet()
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        mutableOnlineUserIds.value = emptySet()
    }

    override fun onAppForegrounded() {
        isForeground.value = true
    }

    override fun onAppBackgrounded() {
        isForeground.value = false
    }

    private suspend fun trackPresence() {
        val userId = authRepository.currentUserId ?: return
        flow { emit(source.fetchOrganizationIds(userId)) }
            .flatMapLatest { organizationIds -> onlineIn(organizationIds, userId) }
            .retryWhen { cause, attempt ->
                Log.w(TAG, "The presence subscription failed, retrying", cause)
                delay(min(RETRY_BASE_DELAY_MS shl min(attempt.toInt(), MAX_BACKOFF_EXPONENT), RETRY_MAX_DELAY_MS))
                true
            }
            .collect { mutableOnlineUserIds.value = it }
    }

    private fun onlineIn(organizationIds: List<String>, userId: String): Flow<Set<String>> {
        if (organizationIds.isEmpty()) return emptyFlow()
        return combine(organizationIds.map { source.session(it, userId) }) { perOrganization ->
            perOrganization.fold(emptySet()) { all, online -> all + online }
        }
    }
}
