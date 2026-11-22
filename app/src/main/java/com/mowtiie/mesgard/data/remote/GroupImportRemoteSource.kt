package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

private const val IMPORT_FUNCTION = "import_groups"

@Serializable
internal data class GroupImportRowDto(
    val line: Int,
    val group: String,
    val parent: String? = null,
    val kind: String,
    val email: String? = null,
    val role: String,
)

@Serializable
internal data class GroupImportRowResultDto(
    val line: Int,
    val group: String? = null,
    val member: String? = null,
    val error: String? = null,
)

@Serializable
internal data class GroupImportResultDto(
    @SerialName("created_groups") val createdGroups: Int,
    @SerialName("added_members") val addedMembers: Int,
    val rows: List<GroupImportRowResultDto>,
)

internal interface GroupImportRemoteSource {
    suspend fun import(orgId: String, rows: List<GroupImportRowDto>): GroupImportResultDto
}

internal class SupabaseGroupImportRemoteSource @Inject constructor(private val client: SupabaseClient) :
    GroupImportRemoteSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun import(orgId: String, rows: List<GroupImportRowDto>): GroupImportResultDto =
        json.decodeFromString(
            client.postgrest.rpc(
                IMPORT_FUNCTION,
                buildJsonObject {
                    put("target_org_id", orgId)
                    put("import_rows", json.encodeToJsonElement(rows))
                },
            ).data,
        )
}
