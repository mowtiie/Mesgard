package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PINS_TABLE = "pinned_messages"

@Serializable
internal data class PinnedRowDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("pinned_at") val pinnedAt: String,
    val messages: PinnedMessageDto,
)

@Serializable
internal data class PinnedMessageDto(val body: String, val sender: SenderNameDto)

@Serializable
internal data class SenderNameDto(@SerialName("display_name") val displayName: String)

@Serializable
private data class NewPinDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("org_id") val orgId: String,
    @SerialName("conversation_id") val conversationId: String,
)

internal interface PinRemoteSource {
    suspend fun fetch(conversationId: String): List<PinnedRowDto>

    suspend fun pin(conversationId: String, orgId: String, messageId: String)

    suspend fun unpin(messageId: String)
}

internal class SupabasePinRemoteSource @Inject constructor(private val client: SupabaseClient) : PinRemoteSource {
    override suspend fun fetch(conversationId: String): List<PinnedRowDto> = client.from(PINS_TABLE).select(
        Columns.raw(
            "message_id, pinned_at, messages!inner(body, sender:profiles!messages_sender_id_fkey(display_name))",
        ),
    ) {
        filter { eq("conversation_id", conversationId) }
        order("pinned_at", Order.DESCENDING)
    }.decodeList<PinnedRowDto>()

    override suspend fun pin(conversationId: String, orgId: String, messageId: String) {
        client.from(PINS_TABLE).insert(NewPinDto(messageId, orgId, conversationId))
    }

    override suspend fun unpin(messageId: String) {
        client.from(PINS_TABLE).delete { filter { eq("message_id", messageId) } }
    }
}
