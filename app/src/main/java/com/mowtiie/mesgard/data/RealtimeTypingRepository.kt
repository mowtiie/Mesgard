package com.mowtiie.mesgard.data

import android.util.Log
import com.mowtiie.mesgard.data.local.ProfileDao
import com.mowtiie.mesgard.data.local.ProfileEntity
import com.mowtiie.mesgard.data.remote.ProfileRemoteSource
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.TypingRemoteSource
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.TypingRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "TypingRepository"
internal const val TYPING_THROTTLE_MS = 2_500L
internal const val TYPING_EXPIRY_MS = 5_000L

internal class RealtimeTypingRepository @Inject constructor(
    private val remoteSource: TypingRemoteSource,
    private val profileSource: ProfileRemoteSource,
    private val profileDao: ProfileDao,
    private val authRepository: AuthRepository,
    private val clock: Clock,
) : TypingRepository {
    override fun observeTypingNames(conversationId: String, ownTyping: Flow<Unit>): Flow<List<String>> {
        val userId = authRepository.currentUserId ?: return emptyFlow()
        return typingNames(conversationId, userId, ownTyping).catch { exception ->
            if (exception is CancellationException) throw exception
            Log.i(TAG, "Typing indicators are unavailable", exception)
            emit(emptyList())
        }
    }

    private fun typingNames(conversationId: String, userId: String, ownTyping: Flow<Unit>) = channelFlow {
        val typers = LinkedHashMap<String, Pair<String, Job>>()
        val lock = Mutex()
        remoteSource.session(conversationId, userId, ownTyping.throttled()).collect { typerId ->
            if (typerId == userId) return@collect
            val name = displayNameOf(typerId) ?: return@collect
            lock.withLock {
                typers.remove(typerId)?.second?.cancel()
                typers[typerId] = name to launch {
                    delay(TYPING_EXPIRY_MS)
                    lock.withLock {
                        typers.remove(typerId)
                        send(typers.values.map { it.first })
                    }
                }
                send(typers.values.map { it.first })
            }
        }
    }

    private fun Flow<Unit>.throttled(): Flow<Unit> = flow {
        var lastSentAt = Long.MIN_VALUE
        collect {
            val now = clock.millis()
            if (lastSentAt == Long.MIN_VALUE || now - lastSentAt >= TYPING_THROTTLE_MS) {
                lastSentAt = now
                emit(Unit)
            }
        }
    }

    private suspend fun displayNameOf(userId: String): String? =
        profileDao.findDisplayName(userId) ?: fetchDisplayName(userId)

    private suspend fun fetchDisplayName(userId: String): String? {
        val outcome = attemptRemote(TAG) { profileSource.fetch(userId) }
        val profile = (outcome as? RemoteOutcome.Success)?.value ?: return null
        profileDao.upsertAll(listOf(ProfileEntity(profile.id, profile.displayName)))
        return profile.displayName
    }
}
