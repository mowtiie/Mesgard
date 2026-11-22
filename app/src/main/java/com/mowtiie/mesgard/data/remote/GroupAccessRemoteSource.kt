package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val OPEN_FUNCTION = "open_group_messages"
private const val READ_FUNCTION = "read_group_messages"
private const val NOTICES_FUNCTION = "list_group_access_notices"

@Serializable
internal data class AccessedMessageDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("sender_name") val senderName: String? = null,
    val body: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("has_attachments") val hasAttachments: Boolean = false,
)

@Serializable
internal data class OpeningDto(@SerialName("opened_at") val openedAt: String)

internal interface GroupAccessRemoteSource {
    suspend fun open(groupId: String, reason: String)

    suspend fun messages(
        groupId: String,
        beforeCreatedAt: String?,
        beforeId: String?,
        pageSize: Int,
    ): List<AccessedMessageDto>

    suspend fun openings(groupId: String): List<OpeningDto>
}

internal class SupabaseGroupAccessRemoteSource @Inject constructor(private val client: SupabaseClient) :
    GroupAccessRemoteSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun open(groupId: String, reason: String) {
        client.postgrest.rpc(
            OPEN_FUNCTION,
            buildJsonObject {
                put("target_group_id", groupId)
                put("access_reason", reason)
            },
        )
    }

    override suspend fun messages(
        groupId: String,
        beforeCreatedAt: String?,
        beforeId: String?,
        pageSize: Int,
    ): List<AccessedMessageDto> = json.decodeFromString(
        client.postgrest.rpc(
            READ_FUNCTION,
            buildJsonObject {
                put("target_group_id", groupId)
                put("page_size", pageSize)
                put("before_created_at", beforeCreatedAt)
                put("before_message_id", beforeId)
            },
        ).data,
    )

    override suspend fun openings(groupId: String): List<OpeningDto> = json.decodeFromString(
        client.postgrest.rpc(NOTICES_FUNCTION, buildJsonObject { put("target_group_id", groupId) }).data,
    )
}
