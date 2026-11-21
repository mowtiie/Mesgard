package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val USER_SETTINGS_TABLE = "user_settings"
private const val SET_FUNCTION = "set_online_status_visibility"

@Serializable
internal data class OnlineStatusDto(@SerialName("show_online_status") val showOnlineStatus: Boolean? = null)

internal interface OnlineStatusRemoteSource {
    suspend fun fetchChoice(userId: String): Boolean?

    suspend fun saveChoice(show: Boolean)
}

internal class SupabaseOnlineStatusRemoteSource @Inject constructor(private val client: SupabaseClient) :
    OnlineStatusRemoteSource {
    override suspend fun fetchChoice(userId: String): Boolean? = client.from(USER_SETTINGS_TABLE)
        .select { filter { eq("user_id", userId) } }
        .decodeSingleOrNull<OnlineStatusDto>()
        ?.showOnlineStatus

    override suspend fun saveChoice(show: Boolean) {
        client.postgrest.rpc(SET_FUNCTION, buildJsonObject { put("new_visibility", show) })
    }
}
