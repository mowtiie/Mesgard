package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

private const val IS_MFA_REQUIRED_FUNCTION = "is_mfa_required"

internal interface MfaRemoteSource {
    /** Whether the server holds this account to a second factor. It answers at aal1, when nothing else is readable. */
    suspend fun isRequired(): Boolean
}

internal class SupabaseMfaRemoteSource @Inject constructor(private val client: SupabaseClient) : MfaRemoteSource {
    override suspend fun isRequired(): Boolean = client.postgrest.rpc(IS_MFA_REQUIRED_FUNCTION).decodeAs<Boolean>()
}
