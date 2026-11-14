package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val CREATE_ANNOUNCEMENT_FUNCTION = "create_announcement_conversation"

internal interface AnnouncementRemoteSource {
    suspend fun createAnnouncementConversation(orgId: String): String
}

internal class SupabaseAnnouncementRemoteSource @Inject constructor(private val client: SupabaseClient) :
    AnnouncementRemoteSource {
    override suspend fun createAnnouncementConversation(orgId: String): String = client.postgrest
        .rpc(CREATE_ANNOUNCEMENT_FUNCTION, buildJsonObject { put("target_org_id", orgId) })
        .decodeAs<String>()
}
