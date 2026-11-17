package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val SERVER_INFO_FUNCTION = "server_info"

@Serializable
internal data class ServerInfoDto(@SerialName("api_level") val apiLevel: Int)

internal interface ServerInfoRemoteSource {
    suspend fun readInfo(): ServerInfoDto
}

internal class SupabaseServerInfoRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ServerInfoRemoteSource {
    override suspend fun readInfo(): ServerInfoDto = client.postgrest.rpc(SERVER_INFO_FUNCTION).decodeAs()
}
