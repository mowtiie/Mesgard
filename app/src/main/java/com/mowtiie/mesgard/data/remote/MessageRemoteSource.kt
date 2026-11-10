package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.repository.MESSAGE_PAGE_SIZE
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val MESSAGES_TABLE = "messages"
private const val MARK_READ_FUNCTION = "mark_conversation_read"
private val MESSAGE_COLUMNS = Columns.raw("*, sender:profiles!messages_sender_id_fkey(id, display_name)")

internal interface MessageRemoteSource {
    suspend fun fetchLatest(conversationId: String): List<MessageDto>

    suspend fun fetchOlder(conversationId: String, beforeEpochMillis: Long): List<MessageDto>

    suspend fun send(message: NewMessageDto): MessageDto

    suspend fun markConversationRead(conversationId: String, upToMessageId: String)
}

internal class SupabaseMessageRemoteSource @Inject constructor(private val client: SupabaseClient) :
    MessageRemoteSource {
    override suspend fun fetchLatest(conversationId: String): List<MessageDto> =
        client.from(MESSAGES_TABLE).select(MESSAGE_COLUMNS) {
            filter { eq("conversation_id", conversationId) }
            order("created_at", Order.DESCENDING)
            limit(MESSAGE_PAGE_SIZE.toLong())
        }.decodeList<MessageDto>()

    override suspend fun fetchOlder(conversationId: String, beforeEpochMillis: Long): List<MessageDto> =
        client.from(MESSAGES_TABLE).select(MESSAGE_COLUMNS) {
            filter {
                eq("conversation_id", conversationId)
                lt("created_at", Instant.ofEpochMilli(beforeEpochMillis).toString())
            }
            order("created_at", Order.DESCENDING)
            limit(MESSAGE_PAGE_SIZE.toLong())
        }.decodeList<MessageDto>()

    override suspend fun send(message: NewMessageDto): MessageDto {
        client.from(MESSAGES_TABLE).upsert(message) {
            onConflict = "id"
            ignoreDuplicates = true
        }
        return client.from(MESSAGES_TABLE).select(MESSAGE_COLUMNS) {
            filter { eq("id", message.id) }
        }.decodeSingle<MessageDto>()
    }

    override suspend fun markConversationRead(conversationId: String, upToMessageId: String) {
        client.postgrest.rpc(
            MARK_READ_FUNCTION,
            buildJsonObject {
                put("target_conversation_id", conversationId)
                put("up_to_message_id", upToMessageId)
            },
        )
    }
}
