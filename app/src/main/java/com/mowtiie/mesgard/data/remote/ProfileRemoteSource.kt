package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val PROFILES_TABLE = "profiles"

internal interface ProfileRemoteSource {
    suspend fun fetch(userId: String): SenderDto?
}

internal class SupabaseProfileRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ProfileRemoteSource {
    override suspend fun fetch(userId: String): SenderDto? = client.from(PROFILES_TABLE)
        .select { filter { eq("id", userId) } }
        .decodeSingleOrNull<SenderDto>()
}
