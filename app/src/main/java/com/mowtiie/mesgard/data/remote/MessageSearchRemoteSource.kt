package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.model.SearchCursor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val SEARCH_FUNCTION = "search_messages"

@Serializable
internal data class SearchHitDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("sender_name") val senderName: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
)

internal interface MessageSearchRemoteSource {
    suspend fun search(conversationId: String, query: String, before: SearchCursor?, limit: Int): List<SearchHitDto>
}

internal class SupabaseMessageSearchRemoteSource @Inject constructor(private val client: SupabaseClient) :
    MessageSearchRemoteSource {
    override suspend fun search(
        conversationId: String,
        query: String,
        before: SearchCursor?,
        limit: Int,
    ): List<SearchHitDto> = client.postgrest.rpc(
        SEARCH_FUNCTION,
        buildJsonObject {
            put("target_conversation_id", conversationId)
            put("search_text", query)
            put("page_size", limit)
            if (before != null) {
                put("before_created_at", before.createdAt)
                put("before_message_id", before.messageId)
            }
        },
    ).decodeList()
}
