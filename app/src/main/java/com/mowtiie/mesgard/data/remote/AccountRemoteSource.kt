package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PROFILES_TABLE = "profiles"
private const val MEMBERS_TABLE = "org_members"
private val MEMBERSHIP_COLUMNS = Columns.raw("org_id, role, organization:organizations(name)")

@Serializable
internal data class ProfileNameDto(@SerialName("display_name") val displayName: String)

@Serializable
internal data class OrganizationNameDto(val name: String)

@Serializable
internal data class AccountMembershipDto(
    @SerialName("org_id") val orgId: String,
    val role: String,
    val organization: OrganizationNameDto,
)

internal interface AccountRemoteSource {
    suspend fun fetchDisplayName(userId: String): String?

    suspend fun fetchMemberships(userId: String): List<AccountMembershipDto>
}

internal class SupabaseAccountRemoteSource @Inject constructor(private val client: SupabaseClient) :
    AccountRemoteSource {
    override suspend fun fetchDisplayName(userId: String): String? = client.from(PROFILES_TABLE)
        .select(Columns.list("display_name")) { filter { eq("id", userId) } }
        .decodeSingleOrNull<ProfileNameDto>()
        ?.displayName

    override suspend fun fetchMemberships(userId: String): List<AccountMembershipDto> =
        client.from(MEMBERS_TABLE).select(MEMBERSHIP_COLUMNS) {
            filter { eq("user_id", userId) }
        }.decodeList<AccountMembershipDto>()
}
