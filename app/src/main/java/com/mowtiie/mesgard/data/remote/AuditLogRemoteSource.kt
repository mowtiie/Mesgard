package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.model.AuditCursor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

private const val AUDIT_TABLE = "audit_logs"

@Serializable
internal data class AuditRowDto(
    val id: String,
    @SerialName("actor_id") val actorId: String,
    val action: String,
    @SerialName("target_id") val targetId: String? = null,
    val metadata: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: String,
)

internal interface AuditLogRemoteSource {
    suspend fun fetchPage(orgId: String, before: AuditCursor?, limit: Int): List<AuditRowDto>
}

internal class SupabaseAuditLogRemoteSource @Inject constructor(private val client: SupabaseClient) :
    AuditLogRemoteSource {
    /** Keyset paging on (created_at, id): entries of one transaction share a timestamp, so the id breaks ties. */
    override suspend fun fetchPage(orgId: String, before: AuditCursor?, limit: Int): List<AuditRowDto> =
        client.from(AUDIT_TABLE).select(
            Columns.list("id", "actor_id", "action", "target_id", "metadata", "created_at"),
        ) {
            filter {
                eq("org_id", orgId)
                if (before != null) {
                    or {
                        lt("created_at", before.createdAt)
                        and {
                            eq("created_at", before.createdAt)
                            lt("id", before.id)
                        }
                    }
                }
            }
            order("created_at", Order.DESCENDING)
            order("id", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList()
}
