package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

private const val COMPLETE_PASSWORD_CHANGE_FUNCTION = "complete_password_change"

internal interface PasswordRemoteSource {
    /** Removes the mark that makes the app ask for a new password after an admin set a temporary one. */
    suspend fun clearChangeRequirement()
}

internal class SupabasePasswordRemoteSource @Inject constructor(private val client: SupabaseClient) :
    PasswordRemoteSource {
    override suspend fun clearChangeRequirement() {
        client.postgrest.rpc(COMPLETE_PASSWORD_CHANGE_FUNCTION)
    }
}
