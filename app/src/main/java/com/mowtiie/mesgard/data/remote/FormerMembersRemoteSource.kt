package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.model.FormerMemberCursor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LIST_FUNCTION = "list_former_members"

@Serializable
internal data class FormerMemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("removed_at") val removedAt: String,
    @SerialName("is_erased") val isErased: Boolean,
    @SerialName("can_erase") val canErase: Boolean,
)

internal interface FormerMembersRemoteSource {
    suspend fun fetchPage(orgId: String, before: FormerMemberCursor?, limit: Int): List<FormerMemberDto>
}

internal class SupabaseFormerMembersRemoteSource @Inject constructor(private val client: SupabaseClient) :
    FormerMembersRemoteSource {
    override suspend fun fetchPage(orgId: String, before: FormerMemberCursor?, limit: Int): List<FormerMemberDto> =
        client.postgrest.rpc(
            LIST_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("page_size", limit)
                if (before != null) {
                    put("removed_before", before.removedAt)
                    put("before_user_id", before.userId)
                }
            },
        ).decodeList()
}
