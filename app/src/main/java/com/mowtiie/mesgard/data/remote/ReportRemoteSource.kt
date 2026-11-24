package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val SUBMIT_FUNCTION = "submit_report"

internal data class SubmitReportRequest(
    val reportId: String,
    val orgId: String,
    val category: String,
    val subject: String,
    val messageId: String?,
    val userId: String?,
    val note: String?,
    val isAnonymous: Boolean,
)

internal interface ReportRemoteSource {
    suspend fun submit(request: SubmitReportRequest)
}

internal class SupabaseReportRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ReportRemoteSource {
    override suspend fun submit(request: SubmitReportRequest) {
        client.postgrest.rpc(
            SUBMIT_FUNCTION,
            buildJsonObject {
                put("new_report_id", request.reportId)
                put("target_org_id", request.orgId)
                put("report_category", request.category)
                put("report_subject", request.subject)
                put("target_message_id", request.messageId)
                put("target_user_id", request.userId)
                put("report_note", request.note)
                put("report_anonymous", request.isAnonymous)
            },
        )
    }
}
