package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal const val CONTACT_LIMIT = 200
private const val MEMBERS_TABLE = "org_members"
private const val CREATE_DIRECT_FUNCTION = "create_direct_conversation"
private val CONTACT_COLUMNS =
    Columns.raw("org_id, user_id, role, user:profiles!org_members_user_id_fkey(id, display_name)")

internal interface ContactRemoteSource {
    suspend fun fetchContacts(excludingUserId: String?): List<ContactDto>

    suspend fun createDirectConversation(orgId: String, otherUserId: String): String
}

internal class SupabaseContactRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ContactRemoteSource {
    override suspend fun fetchContacts(excludingUserId: String?): List<ContactDto> =
        client.from(MEMBERS_TABLE).select(CONTACT_COLUMNS) {
            filter { if (excludingUserId != null) neq("user_id", excludingUserId) }
            order("created_at", Order.ASCENDING)
            limit(CONTACT_LIMIT.toLong())
        }.decodeList<ContactDto>()

    override suspend fun createDirectConversation(orgId: String, otherUserId: String): String = client.postgrest
        .rpc(
            CREATE_DIRECT_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("other_user_id", otherUserId)
            },
        )
        .decodeAs<String>()
}
