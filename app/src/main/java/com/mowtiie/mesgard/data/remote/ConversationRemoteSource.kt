package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal const val CONVERSATION_PAGE_SIZE = 100
private const val LIST_FUNCTION = "list_conversation_summaries"

internal interface ConversationRemoteSource {
    suspend fun fetchSummaries(): List<ConversationSummaryDto>
}

internal class SupabaseConversationRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ConversationRemoteSource {
    override suspend fun fetchSummaries(): List<ConversationSummaryDto> = client.postgrest
        .rpc(LIST_FUNCTION, buildJsonObject { put("page_size", CONVERSATION_PAGE_SIZE) })
        .decodeList<ConversationSummaryDto>()
}
