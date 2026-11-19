package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal const val CONVERSATION_PAGE_SIZE = 100
private const val LIST_FUNCTION = "list_conversation_summaries"
private const val PARTICIPANTS_TABLE = "conversation_participants"

internal interface ConversationRemoteSource {
    suspend fun fetchSummaries(): List<ConversationSummaryDto>

    /** The conversations the user muted. The summaries do not say, and the user's own row is all that is read. */
    suspend fun fetchMutedConversationIds(userId: String): Set<String>
}

internal class SupabaseConversationRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ConversationRemoteSource {
    override suspend fun fetchSummaries(): List<ConversationSummaryDto> = client.postgrest
        .rpc(LIST_FUNCTION, buildJsonObject { put("page_size", CONVERSATION_PAGE_SIZE) })
        .decodeList<ConversationSummaryDto>()

    override suspend fun fetchMutedConversationIds(userId: String): Set<String> =
        client.from(PARTICIPANTS_TABLE).select(Columns.list("conversation_id")) {
            filter {
                eq("user_id", userId)
                eq("muted", true)
            }
        }.decodeList<MutedConversationDto>().map { it.conversationId }.toSet()
}

@Serializable
private data class MutedConversationDto(@SerialName("conversation_id") val conversationId: String)
