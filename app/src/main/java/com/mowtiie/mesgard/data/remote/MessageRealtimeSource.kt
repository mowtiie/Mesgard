package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext

private const val CHANNEL_NAME = "messages-sync"
private const val SCHEMA = "public"
private const val TABLE = "messages"

internal sealed interface RealtimeEvent {
    data class MessageInserted(val message: MessageDto) : RealtimeEvent

    data class MessageUpdated(val message: MessageDto) : RealtimeEvent

    data object Connected : RealtimeEvent

    data object Disconnected : RealtimeEvent
}

internal interface MessageRealtimeSource {
    fun events(): Flow<RealtimeEvent>
}

internal class SupabaseMessageRealtimeSource @Inject constructor(private val client: SupabaseClient) :
    MessageRealtimeSource {
    override fun events(): Flow<RealtimeEvent> = flow {
        val channel = client.channel(CHANNEL_NAME)
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = SCHEMA) { table = TABLE }
        val updates = channel.postgresChangeFlow<PostgresAction.Update>(schema = SCHEMA) { table = TABLE }
        val connections = channel.status.filter { it == RealtimeChannel.Status.SUBSCRIBED }
        val disconnections = client.realtime.status.filter { it == Realtime.Status.DISCONNECTED }
        try {
            channel.subscribe()
            emitAll(
                merge(
                    inserts.map { RealtimeEvent.MessageInserted(it.decodeRecord<MessageDto>()) },
                    updates.map { RealtimeEvent.MessageUpdated(it.decodeRecord<MessageDto>()) },
                    connections.map { RealtimeEvent.Connected },
                    disconnections.map { RealtimeEvent.Disconnected },
                ),
            )
        } finally {
            withContext(NonCancellable) { client.realtime.removeChannel(channel) }
        }
    }
}
