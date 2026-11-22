package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val CONVERSATIONS_TABLE = "conversations"

@Serializable
internal data class ConversationGroupDto(@SerialName("group_id") val groupId: String? = null)

internal interface ConversationGroupRemoteSource {
    /** The group a group conversation belongs to, or null for any other conversation. */
    suspend fun findGroupId(conversationId: String): String?
}

internal class SupabaseConversationGroupRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ConversationGroupRemoteSource {
    override suspend fun findGroupId(conversationId: String): String? =
        client.from(CONVERSATIONS_TABLE).select(Columns.list("group_id")) {
            filter { eq("id", conversationId) }
        }.decodeList<ConversationGroupDto>().firstOrNull()?.groupId
}
