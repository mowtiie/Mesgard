package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LIST_FUNCTION = "list_profile_notices"
private const val ACKNOWLEDGE_FUNCTION = "acknowledge_profile_notices"

@Serializable
internal data class ProfileNoticeDto(val id: String, val kind: String)

internal interface ProfileNoticeRemoteSource {
    suspend fun unseen(): List<ProfileNoticeDto>

    suspend fun acknowledge(ids: Collection<String>)
}

internal class SupabaseProfileNoticeRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ProfileNoticeRemoteSource {
    override suspend fun unseen(): List<ProfileNoticeDto> =
        client.postgrest.rpc(LIST_FUNCTION).decodeList<ProfileNoticeDto>()

    override suspend fun acknowledge(ids: Collection<String>) {
        client.postgrest.rpc(
            ACKNOWLEDGE_FUNCTION,
            buildJsonObject { put("notice_ids", JsonArray(ids.map(::JsonPrimitive))) },
        )
    }
}
