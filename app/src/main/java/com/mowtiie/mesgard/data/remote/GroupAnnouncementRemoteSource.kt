package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val SEND_FUNCTION = "send_to_group_tree"

internal interface GroupAnnouncementRemoteSource {
    suspend fun sendToGroupTree(groupId: String, messageId: String, body: String): Int
}

internal class SupabaseGroupAnnouncementRemoteSource @Inject constructor(private val client: SupabaseClient) :
    GroupAnnouncementRemoteSource {
    override suspend fun sendToGroupTree(groupId: String, messageId: String, body: String): Int = Json.decodeFromString(
        client.postgrest.rpc(
            SEND_FUNCTION,
            buildJsonObject {
                put("parent_group_id", groupId)
                put("new_message_id", messageId)
                put("new_body", body)
            },
        ).data,
    )
}
