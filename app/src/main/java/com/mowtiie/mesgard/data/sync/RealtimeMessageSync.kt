package com.mowtiie.mesgard.data.sync

import android.util.Log
import com.mowtiie.mesgard.data.local.MesgardDatabase
import com.mowtiie.mesgard.data.local.ProfileEntity
import com.mowtiie.mesgard.data.remote.MessageDto
import com.mowtiie.mesgard.data.remote.MessageRealtimeSource
import com.mowtiie.mesgard.data.remote.ProfileRemoteSource
import com.mowtiie.mesgard.data.remote.RealtimeEvent
import com.mowtiie.mesgard.data.remote.RemoteOutcome
import com.mowtiie.mesgard.data.remote.attemptRemote
import com.mowtiie.mesgard.data.remote.toEntity
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.sync.RealtimeSync
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

private const val TAG = "RealtimeMessageSync"
private const val RETRY_BASE_DELAY_MS = 1_000L
private const val RETRY_MAX_DELAY_MS = 30_000L
private const val MAX_BACKOFF_EXPONENT = 5

@Singleton
internal class RealtimeMessageSync @Inject constructor(
    private val source: MessageRealtimeSource,
    private val profileSource: ProfileRemoteSource,
    database: MesgardDatabase,
    private val conversationRepository: ConversationRepository,
    private val authRepository: AuthRepository,
) : RealtimeSync {
    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val profileDao = database.profileDao()
    private val mutableReconnections = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var job: Job? = null
    private var hasConnected = false

    override val reconnections: SharedFlow<Unit> = mutableReconnections.asSharedFlow()

    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        hasConnected = false
        job = scope.launch {
            source.events()
                .retryWhen { cause, attempt ->
                    Log.w(TAG, "The realtime subscription failed, retrying", cause)
                    delay(min(RETRY_BASE_DELAY_MS shl min(attempt.toInt(), MAX_BACKOFF_EXPONENT), RETRY_MAX_DELAY_MS))
                    true
                }
                .collect { handle(it) }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun handle(event: RealtimeEvent) = when (event) {
        is RealtimeEvent.MessageInserted -> storeMessage(event.message, isNew = true)
        is RealtimeEvent.MessageUpdated -> storeMessage(event.message, isNew = false)
        RealtimeEvent.Connected -> onConnected()
    }

    private suspend fun onConnected() {
        val isReconnection = hasConnected
        hasConnected = true
        if (!isReconnection) return
        conversationRepository.refresh()
        mutableReconnections.tryEmit(Unit)
    }

    private suspend fun storeMessage(message: MessageDto, isNew: Boolean) {
        if (conversationDao.findOrgId(message.conversationId) == null) conversationRepository.refresh()
        ensureSenderProfile(message.senderId)
        val isUnseenIncoming = isNew &&
            message.senderId != authRepository.currentUserId &&
            messageDao.findById(message.id) == null
        if (isUnseenIncoming) conversationDao.incrementUnreadCount(message.conversationId)
        messageDao.upsert(message.toEntity())
    }

    private suspend fun ensureSenderProfile(senderId: String) {
        if (profileDao.exists(senderId)) return
        val outcome = attemptRemote(TAG) { profileSource.fetch(senderId) }
        if (outcome is RemoteOutcome.Success) {
            outcome.value?.let { profileDao.upsertAll(listOf(ProfileEntity(it.id, it.displayName))) }
        }
    }
}
