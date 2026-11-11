package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal const val GROUP_LIMIT = 200
private const val GROUPS_TABLE = "groups"
private const val MEMBERS_TABLE = "org_members"
private const val CREATE_GROUP_CONVERSATION_FUNCTION = "create_group_conversation"

internal interface GroupRemoteSource {
    suspend fun fetchGroups(): List<GroupDto>

    suspend fun fetchMemberships(userId: String): List<MembershipDto>

    suspend fun createGroupConversation(groupId: String): String
}

internal class SupabaseGroupRemoteSource @Inject constructor(private val client: SupabaseClient) : GroupRemoteSource {
    override suspend fun fetchGroups(): List<GroupDto> =
        client.from(GROUPS_TABLE).select(Columns.list("id", "org_id", "name")) {
            order("name", Order.ASCENDING)
            limit(GROUP_LIMIT.toLong())
        }.decodeList<GroupDto>()

    override suspend fun fetchMemberships(userId: String): List<MembershipDto> =
        client.from(MEMBERS_TABLE).select(Columns.list("org_id", "role")) {
            filter { eq("user_id", userId) }
        }.decodeList<MembershipDto>()

    override suspend fun createGroupConversation(groupId: String): String = client.postgrest
        .rpc(CREATE_GROUP_CONVERSATION_FUNCTION, buildJsonObject { put("target_group_id", groupId) })
        .decodeAs<String>()
}
