package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val RULES_FUNCTION = "get_profile_picture_rules"
private const val SET_FUNCTION = "set_profile_picture"
private const val REMOVE_FUNCTION = "remove_profile_picture"
private const val POLICY_FUNCTION = "set_profile_picture_policy"

@Serializable
internal data class ProfilePictureRulesDto(
    val policy: String,
    @SerialName("can_set") val canSet: Boolean,
    val version: Int? = null,
    @SerialName("blocked_until") val blockedUntil: String? = null,
)

internal interface ProfilePictureRemoteSource {
    suspend fun rules(orgId: String): ProfilePictureRulesDto

    suspend fun set(orgId: String, version: Int)

    suspend fun remove(orgId: String)

    suspend fun setPolicy(orgId: String, policy: String)
}

internal class SupabaseProfilePictureRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ProfilePictureRemoteSource {
    override suspend fun rules(orgId: String): ProfilePictureRulesDto =
        Json { ignoreUnknownKeys = true }.decodeFromString(
            client.postgrest.rpc(RULES_FUNCTION, buildJsonObject { put("target_org_id", orgId) }).data,
        )

    override suspend fun set(orgId: String, version: Int) {
        client.postgrest.rpc(
            SET_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("new_version", version)
            },
        )
    }

    override suspend fun remove(orgId: String) {
        client.postgrest.rpc(REMOVE_FUNCTION, buildJsonObject { put("target_org_id", orgId) })
    }

    override suspend fun setPolicy(orgId: String, policy: String) {
        client.postgrest.rpc(
            POLICY_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("new_policy", policy)
            },
        )
    }
}
