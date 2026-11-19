package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val GROUPS_TABLE = "groups"
private const val GROUP_MEMBERS_TABLE = "group_members"

@Serializable
internal data class GroupRowDto(
    val id: String,
    val name: String,
    @SerialName("group_members") val members: List<CountDto> = emptyList(),
    @SerialName("posting_policy") val postingPolicy: String = "everyone",
    @SerialName("includes_everyone") val includesEveryone: Boolean = false,
)

@Serializable
internal data class CountDto(val count: Int)

@Serializable
internal data class GroupMemberRowDto(
    @SerialName("user_id") val userId: String,
    @SerialName("role_in_group") val role: String,
)

@Serializable
internal data class IdDto(val id: String)

@Serializable
internal data class UserIdDto(@SerialName("user_id") val userId: String)

@Serializable
private data class NewGroupDto(
    @SerialName("org_id") val orgId: String,
    val name: String,
    @SerialName("posting_policy") val postingPolicy: String,
    @SerialName("includes_everyone") val includesEveryone: Boolean,
)

@Serializable
private data class NewGroupMemberDto(
    @SerialName("org_id") val orgId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
)

internal interface GroupAdminRemoteSource {
    suspend fun listGroups(orgId: String): List<GroupRowDto>

    suspend fun fetchGroup(groupId: String): GroupRowDto?

    suspend fun fetchGroupMembers(groupId: String): List<GroupMemberRowDto>

    suspend fun createGroup(orgId: String, name: String, postingPolicy: String, includesEveryone: Boolean): String

    /** The writes below return true when a row changed; Row Level Security hides the rows an admin may not change. */
    suspend fun renameGroup(groupId: String, name: String): Boolean

    suspend fun updateSettings(groupId: String, postingPolicy: String, includesEveryone: Boolean): Boolean

    suspend fun deleteGroup(groupId: String): Boolean

    suspend fun addMembers(orgId: String, groupId: String, userIds: List<String>)

    suspend fun removeMember(groupId: String, userId: String): Boolean

    suspend fun changeMemberRole(groupId: String, userId: String, role: String): Boolean
}

internal class SupabaseGroupAdminRemoteSource @Inject constructor(private val client: SupabaseClient) :
    GroupAdminRemoteSource {
    override suspend fun listGroups(orgId: String): List<GroupRowDto> =
        client.from(GROUPS_TABLE).select(Columns.raw("id, name, group_members(count)")) {
            filter { eq("org_id", orgId) }
            order("name", Order.ASCENDING)
        }.decodeList()

    override suspend fun fetchGroup(groupId: String): GroupRowDto? =
        client.from(GROUPS_TABLE).select(Columns.list("id", "name", "posting_policy", "includes_everyone")) {
            filter { eq("id", groupId) }
        }.decodeList<GroupRowDto>().firstOrNull()

    override suspend fun fetchGroupMembers(groupId: String): List<GroupMemberRowDto> =
        client.from(GROUP_MEMBERS_TABLE).select(Columns.list("user_id", "role_in_group")) {
            filter { eq("group_id", groupId) }
        }.decodeList()

    override suspend fun createGroup(
        orgId: String,
        name: String,
        postingPolicy: String,
        includesEveryone: Boolean,
    ): String = client.from(GROUPS_TABLE)
        .insert(NewGroupDto(orgId, name, postingPolicy, includesEveryone)) { select(Columns.list("id")) }
        .decodeSingle<IdDto>().id

    override suspend fun updateSettings(groupId: String, postingPolicy: String, includesEveryone: Boolean): Boolean =
        client.from(GROUPS_TABLE).update(
            {
                set("posting_policy", postingPolicy)
                set("includes_everyone", includesEveryone)
            },
        ) {
            select(Columns.list("id"))
            filter { eq("id", groupId) }
        }.decodeList<IdDto>().isNotEmpty()

    override suspend fun renameGroup(groupId: String, name: String): Boolean =
        client.from(GROUPS_TABLE).update({ set("name", name) }) {
            select(Columns.list("id"))
            filter { eq("id", groupId) }
        }.decodeList<IdDto>().isNotEmpty()

    override suspend fun deleteGroup(groupId: String): Boolean = client.from(GROUPS_TABLE).delete {
        select(Columns.list("id"))
        filter { eq("id", groupId) }
    }.decodeList<IdDto>().isNotEmpty()

    override suspend fun addMembers(orgId: String, groupId: String, userIds: List<String>) {
        client.from(GROUP_MEMBERS_TABLE).insert(userIds.map { NewGroupMemberDto(orgId, groupId, it) })
    }

    override suspend fun removeMember(groupId: String, userId: String): Boolean =
        client.from(GROUP_MEMBERS_TABLE).delete {
            select(Columns.list("user_id"))
            filter {
                eq("group_id", groupId)
                eq("user_id", userId)
            }
        }.decodeList<UserIdDto>().isNotEmpty()

    override suspend fun changeMemberRole(groupId: String, userId: String, role: String): Boolean =
        client.from(GROUP_MEMBERS_TABLE).update({ set("role_in_group", role) }) {
            select(Columns.list("user_id"))
            filter {
                eq("group_id", groupId)
                eq("user_id", userId)
            }
        }.decodeList<UserIdDto>().isNotEmpty()
}
