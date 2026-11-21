package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LIST_FUNCTION = "list_legal_holds"
private const val PLACE_FUNCTION = "place_legal_hold"
private const val RELEASE_FUNCTION = "release_legal_hold"

@Serializable
internal data class LegalHoldDto(
    @SerialName("hold_id") val holdId: String,
    val scope: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    val reason: String,
    @SerialName("placed_by_name") val placedByName: String,
    @SerialName("placed_at") val placedAt: String,
)

internal data class PlaceHoldRequest(val orgId: String, val reason: String, val userId: String?, val groupId: String?)

internal interface LegalHoldRemoteSource {
    suspend fun activeHolds(orgId: String): List<LegalHoldDto>

    suspend fun place(request: PlaceHoldRequest)

    suspend fun release(holdId: String, note: String)
}

internal class SupabaseLegalHoldRemoteSource @Inject constructor(private val client: SupabaseClient) :
    LegalHoldRemoteSource {
    override suspend fun activeHolds(orgId: String): List<LegalHoldDto> = client.postgrest.rpc(
        LIST_FUNCTION,
        buildJsonObject { put("target_org_id", orgId) },
    ).decodeList()

    override suspend fun place(request: PlaceHoldRequest) {
        client.postgrest.rpc(
            PLACE_FUNCTION,
            buildJsonObject {
                put("target_org_id", request.orgId)
                put("hold_reason", request.reason)
                put("target_user_id", request.userId)
                put("target_group_id", request.groupId)
            },
        )
    }

    override suspend fun release(holdId: String, note: String) {
        client.postgrest.rpc(
            RELEASE_FUNCTION,
            buildJsonObject {
                put("target_hold_id", holdId)
                put("note", note)
            },
        )
    }
}
