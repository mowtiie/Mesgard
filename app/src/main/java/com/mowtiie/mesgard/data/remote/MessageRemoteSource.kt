package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.repository.MESSAGE_PAGE_SIZE
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val MESSAGES_TABLE = "messages"
private const val ATTACHMENTS_TABLE = "attachments"
private const val RECEIPTS_TABLE = "message_receipts"
private const val MARK_READ_FUNCTION = "mark_conversation_read"
private val MESSAGE_COLUMNS = Columns.raw(
    "*, sender:profiles!messages_sender_id_fkey(id, display_name), " +
        "attachments(id, storage_path, mime_type, size_bytes, file_name, width, height)",
)

internal interface MessageRemoteSource {
    suspend fun fetchLatest(conversationId: String): List<MessageDto>

    suspend fun fetchOlder(conversationId: String, beforeEpochMillis: Long): List<MessageDto>

    suspend fun send(message: NewMessageDto): MessageDto

    suspend fun fetchAttachments(messageId: String): List<AttachmentDto>

    /** The people who have read the message, the latest first. Only its sender can see them. */
    suspend fun fetchReaders(messageId: String, limit: Int): List<ReceiptDto>

    /** Changes the text of the message. The server stamps the time and archives the earlier text. */
    suspend fun edit(messageId: String, body: String): MessageDto

    /** Soft deletes the message. The server blanks the text and archives the original for administrators. */
    suspend fun delete(messageId: String): MessageDto

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

    override suspend fun fetchReaders(messageId: String, limit: Int): List<ReceiptDto> =
        client.from(RECEIPTS_TABLE).select(Columns.raw("user_id, read_at, profiles(display_name)")) {
            filter {
                eq("message_id", messageId)
                filterNot("read_at", FilterOperator.IS, "null")
            }
            order("read_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<ReceiptDto>()

    override suspend fun edit(messageId: String, body: String): MessageDto {
        val rows = client.from(MESSAGES_TABLE).update({ set("body", body) }) {
            select(MESSAGE_COLUMNS)
            filter { eq("id", messageId) }
        }.decodeList<MessageDto>()
        require(rows.isNotEmpty()) { "No message was changed" }
        return rows.first()
    }

    override suspend fun delete(messageId: String): MessageDto {
        val rows = client.from(MESSAGES_TABLE).update({ set("deleted_at", "now") }) {
            select(MESSAGE_COLUMNS)
            filter { eq("id", messageId) }
        }.decodeList<MessageDto>()
        require(rows.isNotEmpty()) { "No message was deleted" }
        return rows.first()
    }

    override suspend fun fetchAttachments(messageId: String): List<AttachmentDto> =
        client.from(ATTACHMENTS_TABLE).select {
            filter { eq("message_id", messageId) }
        }.decodeList<AttachmentDto>()

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
