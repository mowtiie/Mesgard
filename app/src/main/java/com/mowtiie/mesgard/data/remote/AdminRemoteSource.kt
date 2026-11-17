package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.statement.bodyAsText
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val ADMIN_FUNCTION = "admin-users"
private const val MEMBERS_TABLE = "org_members"

@Serializable
internal data class CreatedMembersDto(val results: List<MemberCreationDto>)

@Serializable
internal data class MemberCreationDto(
    val email: String,
    val status: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("temporary_password") val temporaryPassword: String? = null,
    val reason: String? = null,
)

@Serializable
internal data class PasswordResetDto(@SerialName("temporary_password") val temporaryPassword: String)

@Serializable
internal data class MfaResetDto(@SerialName("factors_removed") val factorsRemoved: Int)

@Serializable
internal data class RemovedMemberDto(val removed: Boolean, @SerialName("login_disabled") val loginDisabled: Boolean)

@Serializable
private data class RoleRow(@SerialName("user_id") val userId: String)

internal data class NewMemberRequest(val email: String, val displayName: String, val role: String)

internal interface AdminRemoteSource {
    suspend fun createMembers(orgId: String, members: List<NewMemberRequest>): CreatedMembersDto

    suspend fun resetPassword(orgId: String, userId: String): PasswordResetDto

    suspend fun removeMember(orgId: String, userId: String): RemovedMemberDto

    suspend fun resetMfa(orgId: String, userId: String): MfaResetDto

    /** Returns true when a row was changed; Row Level Security hides rows the caller may not change. */
    suspend fun changeRole(orgId: String, userId: String, role: String): Boolean
}

internal class SupabaseAdminRemoteSource @Inject constructor(private val client: SupabaseClient) : AdminRemoteSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun createMembers(orgId: String, members: List<NewMemberRequest>): CreatedMembersDto = invoke(
        buildJsonObject {
            put("action", "create_users")
            put("org_id", orgId)
            put("users", usersOf(members))
        },
    )

    override suspend fun resetPassword(orgId: String, userId: String): PasswordResetDto =
        invoke(targetRequest("reset_password", orgId, userId))

    override suspend fun removeMember(orgId: String, userId: String): RemovedMemberDto =
        invoke(targetRequest("remove_member", orgId, userId))

    override suspend fun resetMfa(orgId: String, userId: String): MfaResetDto =
        invoke(targetRequest("reset_mfa", orgId, userId))

    override suspend fun changeRole(orgId: String, userId: String, role: String): Boolean =
        client.from(MEMBERS_TABLE).update({ set("role", role) }) {
            select()
            filter {
                eq("org_id", orgId)
                eq("user_id", userId)
            }
        }.decodeList<RoleRow>().isNotEmpty()

    private suspend inline fun <reified T> invoke(body: JsonObject): T {
        val response = client.functions.invoke(function = ADMIN_FUNCTION, body = body)
        return json.decodeFromString(response.bodyAsText())
    }

    private fun targetRequest(action: String, orgId: String, userId: String) = buildJsonObject {
        put("action", action)
        put("org_id", orgId)
        put("user_id", userId)
    }

    private fun usersOf(members: List<NewMemberRequest>): JsonArray = buildJsonArray {
        members.forEach { member ->
            add(
                buildJsonObject {
                    put("email", member.email)
                    put("display_name", member.displayName)
                    put("role", member.role)
                },
            )
        }
    }
}
