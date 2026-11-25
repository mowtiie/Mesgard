package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val RULES_FUNCTION = "get_group_picture_rules"
private const val SET_FUNCTION = "set_group_picture"
private const val REMOVE_FUNCTION = "remove_group_picture"

@Serializable
internal data class GroupPictureRulesDto(
    @SerialName("org_id") val orgId: String,
    val name: String,
    @SerialName("can_set") val canSet: Boolean,
    @SerialName("can_remove") val canRemove: Boolean,
    val version: Int? = null,
)

internal interface GroupPictureRemoteSource {
    suspend fun rules(groupId: String): GroupPictureRulesDto

    suspend fun set(groupId: String, version: Int)

    suspend fun remove(groupId: String)
}

internal class SupabaseGroupPictureRemoteSource @Inject constructor(private val client: SupabaseClient) :
    GroupPictureRemoteSource {
    override suspend fun rules(groupId: String): GroupPictureRulesDto =
        Json { ignoreUnknownKeys = true }.decodeFromString(
            client.postgrest.rpc(RULES_FUNCTION, buildJsonObject { put("target_group_id", groupId) }).data,
        )

    override suspend fun set(groupId: String, version: Int) {
        client.postgrest.rpc(
            SET_FUNCTION,
            buildJsonObject {
                put("target_group_id", groupId)
                put("new_version", version)
            },
        )
    }

    override suspend fun remove(groupId: String) {
        client.postgrest.rpc(REMOVE_FUNCTION, buildJsonObject { put("target_group_id", groupId) })
    }
}
