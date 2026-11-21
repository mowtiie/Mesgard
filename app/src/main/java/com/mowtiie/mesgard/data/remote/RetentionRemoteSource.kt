package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val GET_FUNCTION = "get_message_retention"
private const val SET_FUNCTION = "set_message_retention"

internal interface RetentionRemoteSource {
    suspend fun retentionDays(orgId: String): Int?

    suspend fun setRetentionDays(orgId: String, days: Int?)
}

internal class SupabaseRetentionRemoteSource @Inject constructor(private val client: SupabaseClient) :
    RetentionRemoteSource {
    override suspend fun retentionDays(orgId: String): Int? = Json.decodeFromString(
        client.postgrest.rpc(GET_FUNCTION, buildJsonObject { put("target_org_id", orgId) }).data,
    )

    override suspend fun setRetentionDays(orgId: String, days: Int?) {
        client.postgrest.rpc(
            SET_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("retention_days", days)
            },
        )
    }
}
