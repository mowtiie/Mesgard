package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject

private const val PARTICIPANTS_TABLE = "conversation_participants"

internal interface ConversationPreferencesRemoteSource {
    /** Writes the columns of the signed in user own participant rows of all the conversations in one request. */
    suspend fun update(userId: String, conversationIds: Collection<String>, changes: JsonObject)
}

internal class SupabaseConversationPreferencesRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ConversationPreferencesRemoteSource {
    override suspend fun update(userId: String, conversationIds: Collection<String>, changes: JsonObject) {
        client.from(PARTICIPANTS_TABLE).update(changes) {
            filter {
                eq("user_id", userId)
                isIn("conversation_id", conversationIds.toList())
            }
        }
    }
}
