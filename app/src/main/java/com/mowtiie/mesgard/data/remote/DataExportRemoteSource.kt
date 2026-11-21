package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put

private const val OVERVIEW_FUNCTION = "export_person_overview"
private const val MESSAGES_FUNCTION = "export_person_messages"

/** Where the next page of messages starts: after the last message of the page before. */
internal data class ExportCursor(val createdAt: String, val messageId: String)

/** The answers are kept as JSON, because they go into the file as they are, without being read by the app. */
internal interface DataExportRemoteSource {
    suspend fun overview(orgId: String, userId: String): JsonObject

    suspend fun messages(orgId: String, userId: String, before: ExportCursor?, pageSize: Int): JsonArray
}

internal class SupabaseDataExportRemoteSource @Inject constructor(private val client: SupabaseClient) :
    DataExportRemoteSource {
    private val json = Json

    override suspend fun overview(orgId: String, userId: String): JsonObject = json.decodeFromString(
        client.postgrest.rpc(
            OVERVIEW_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("target_user_id", userId)
            },
        ).data,
    )

    override suspend fun messages(orgId: String, userId: String, before: ExportCursor?, pageSize: Int): JsonArray =
        json.parseToJsonElement(
            client.postgrest.rpc(
                MESSAGES_FUNCTION,
                buildJsonObject {
                    put("target_org_id", orgId)
                    put("target_user_id", userId)
                    put("page_size", pageSize)
                    if (before != null) {
                        put("before_created_at", before.createdAt)
                        put("before_message_id", before.messageId)
                    }
                },
            ).data,
        ).jsonArray
}
