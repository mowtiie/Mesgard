package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val GET_FUNCTION = "get_group_creation"
private const val SET_FUNCTION = "set_group_creation_policy"

@Serializable
internal data class GroupCreationDto(
    val policy: String,
    @SerialName("can_create") val canCreate: Boolean,
    val created: Int,
    val limit: Int,
)

internal interface GroupCreationRemoteSource {
    suspend fun rules(orgId: String): GroupCreationDto

    suspend fun setPolicy(orgId: String, policy: String)
}

internal class SupabaseGroupCreationRemoteSource @Inject constructor(private val client: SupabaseClient) :
    GroupCreationRemoteSource {
    override suspend fun rules(orgId: String): GroupCreationDto = Json { ignoreUnknownKeys = true }.decodeFromString(
        client.postgrest.rpc(GET_FUNCTION, buildJsonObject { put("target_org_id", orgId) }).data,
    )

    override suspend fun setPolicy(orgId: String, policy: String) {
        client.postgrest.rpc(
            SET_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("new_policy", policy)
            },
        )
    }
}
