package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val USER_SETTINGS_TABLE = "user_settings"
private const val ORGANIZATIONS_TABLE = "organizations"

@Serializable
internal data class UserSettingsDto(
    @SerialName("user_id") val userId: String,
    @SerialName("notification_preview") val notificationPreview: String?,
)

@Serializable
internal data class OrganizationPreviewDto(
    @SerialName("notification_preview_default") val notificationPreviewDefault: String,
)

internal interface SettingsRemoteSource {
    suspend fun fetchUserPreview(userId: String): String?

    suspend fun fetchOrganizationPreviewDefault(): String?

    suspend fun saveUserPreview(userId: String, preview: String)
}

internal class SupabaseSettingsRemoteSource @Inject constructor(private val client: SupabaseClient) :
    SettingsRemoteSource {
    override suspend fun fetchUserPreview(userId: String): String? = client.from(USER_SETTINGS_TABLE)
        .select { filter { eq("user_id", userId) } }
        .decodeSingleOrNull<UserSettingsDto>()
        ?.notificationPreview

    override suspend fun fetchOrganizationPreviewDefault(): String? = client.from(ORGANIZATIONS_TABLE)
        .select(Columns.list("notification_preview_default")) { limit(1) }
        .decodeList<OrganizationPreviewDto>()
        .firstOrNull()
        ?.notificationPreviewDefault

    override suspend fun saveUserPreview(userId: String, preview: String) {
        client.from(USER_SETTINGS_TABLE).upsert(UserSettingsDto(userId, preview)) { onConflict = "user_id" }
    }
}
