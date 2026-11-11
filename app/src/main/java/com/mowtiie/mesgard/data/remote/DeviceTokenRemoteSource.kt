package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TOKENS_TABLE = "device_tokens"
private const val REGISTER_FUNCTION = "register_device_token"

internal interface DeviceTokenRemoteSource {
    suspend fun register(token: String)

    suspend fun unregister(token: String)
}

internal class SupabaseDeviceTokenRemoteSource @Inject constructor(private val client: SupabaseClient) :
    DeviceTokenRemoteSource {
    override suspend fun register(token: String) {
        client.postgrest.rpc(REGISTER_FUNCTION, buildJsonObject { put("new_token", token) })
    }

    override suspend fun unregister(token: String) {
        client.from(TOKENS_TABLE).delete { filter { eq("fcm_token", token) } }
    }
}
