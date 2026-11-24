package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val RULES_FUNCTION = "get_username_rules"
private const val CHANGE_FUNCTION = "set_username"
private const val POLICY_FUNCTION = "set_username_policy"
private const val RESET_FUNCTION = "reset_username"

@Serializable
internal data class UsernameRulesDto(
    val policy: String,
    val username: String,
    @SerialName("policy_allows") val policyAllows: Boolean,
    @SerialName("next_change_at") val nextChangeAt: String? = null,
)

internal interface UsernameRemoteSource {
    suspend fun rules(orgId: String): UsernameRulesDto

    suspend fun change(orgId: String, username: String)

    suspend fun setPolicy(orgId: String, policy: String)

    suspend fun reset(orgId: String, userId: String)
}

internal class SupabaseUsernameRemoteSource @Inject constructor(private val client: SupabaseClient) :
    UsernameRemoteSource {
    override suspend fun rules(orgId: String): UsernameRulesDto = Json { ignoreUnknownKeys = true }.decodeFromString(
        client.postgrest.rpc(RULES_FUNCTION, buildJsonObject { put("target_org_id", orgId) }).data,
    )

    override suspend fun change(orgId: String, username: String) {
        client.postgrest.rpc(
            CHANGE_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("new_username", username)
            },
        )
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

    override suspend fun reset(orgId: String, userId: String) {
        client.postgrest.rpc(
            RESET_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("target_user_id", userId)
            },
        )
    }
}
