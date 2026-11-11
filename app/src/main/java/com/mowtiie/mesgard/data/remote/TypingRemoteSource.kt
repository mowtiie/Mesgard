package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TOPIC_PREFIX = "conversation:"
private const val TYPING_EVENT = "typing"

@Serializable
internal data class TypingPayload(@SerialName("user_id") val userId: String)

internal interface TypingRemoteSource {
    /**
     * Joins the private channel of the conversation, broadcasts a typing event for every item of [signals] and emits
     * the identifier of each user that is reported as typing. The channel is left when the collection ends.
     */
    fun session(conversationId: String, userId: String, signals: Flow<Unit>): Flow<String>
}

internal class SupabaseTypingRemoteSource @Inject constructor(private val client: SupabaseClient) :
    TypingRemoteSource {
    override fun session(conversationId: String, userId: String, signals: Flow<Unit>): Flow<String> = channelFlow {
        val channel = client.channel(TOPIC_PREFIX + conversationId) { isPrivate = true }
        val typing = channel.broadcastFlow<TypingPayload>(TYPING_EVENT)
        try {
            channel.subscribe(blockUntilSubscribed = true)
            launch { signals.collect { channel.broadcast(TYPING_EVENT, TypingPayload(userId)) } }
            typing.collect { send(it.userId) }
        } finally {
            withContext(NonCancellable) { client.realtime.removeChannel(channel) }
        }
    }
}
