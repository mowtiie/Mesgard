package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.model.ReportCursor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val CAN_REVIEW_FUNCTION = "can_review_reports"
private const val LIST_FUNCTION = "list_reports"
private const val GET_FUNCTION = "get_report"
private const val UPDATE_FUNCTION = "update_report"
private const val REVEAL_FUNCTION = "reveal_report_reporter"
private const val LIST_REVIEWERS_FUNCTION = "list_report_reviewers"
private const val SET_REVIEWERS_FUNCTION = "set_report_reviewers"

@Serializable
internal data class ReportRowDto(
    @SerialName("report_id") val reportId: String,
    val category: String,
    val subject: String = "message",
    val status: String,
    @SerialName("subject_user_id") val subjectUserId: String? = null,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("message_id") val messageId: String? = null,
    val anonymous: Boolean,
    @SerialName("reporter_name") val reporterName: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class EvidenceLineDto(
    @SerialName("sender_name") val senderName: String,
    val body: String,
    @SerialName("sent_at") val sentAt: String,
)

@Serializable
internal data class EvidenceDto(
    @SerialName("conversation_title") val conversationTitle: String? = null,
    @SerialName("sender_name") val senderName: String,
    val body: String,
    @SerialName("sent_at") val sentAt: String,
    val context: List<EvidenceLineDto> = emptyList(),
)

@Serializable
internal data class EvidenceFileDto(
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("storage_path") val storagePath: String = "",
)

@Serializable
internal data class ReportEventDto(
    val action: String,
    val note: String? = null,
    @SerialName("actor_name") val actorName: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class ReportDetailDto(
    val id: String,
    val category: String,
    val subject: String = "message",
    val status: String,
    val note: String? = null,
    val anonymous: Boolean,
    @SerialName("reporter_name") val reporterName: String? = null,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("subject_text") val subjectText: String? = null,
    @SerialName("message_removed") val messageRemoved: Boolean = false,
    @SerialName("resolution_note") val resolutionNote: String? = null,
    @SerialName("created_at") val createdAt: String,
    val evidence: EvidenceDto? = null,
    val files: List<EvidenceFileDto> = emptyList(),
    val events: List<ReportEventDto> = emptyList(),
)

@Serializable
internal data class ReviewerDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
)

@Serializable
internal data class RevealedReporterDto(@SerialName("reporter_name") val reporterName: String? = null)

internal data class ReportDecisionRequest(
    val reportId: String,
    val status: String,
    val note: String?,
    val removeMessage: Boolean,
    val resetUsername: Boolean = false,
    val removePicture: Boolean = false,
    val blockDays: Int? = null,
    val renameGroupTo: String? = null,
)

internal interface ReportReviewRemoteSource {
    suspend fun canReview(orgId: String): Boolean

    suspend fun list(orgId: String, status: String, before: ReportCursor?, limit: Int): List<ReportRowDto>

    suspend fun detail(reportId: String): ReportDetailDto

    suspend fun decide(request: ReportDecisionRequest)

    suspend fun revealReporter(reportId: String, reason: String): RevealedReporterDto

    suspend fun reviewers(orgId: String): List<ReviewerDto>

    suspend fun setReviewers(orgId: String, userIds: List<String>)
}

internal class SupabaseReportReviewRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ReportReviewRemoteSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun canReview(orgId: String): Boolean = client.postgrest.rpc(
        CAN_REVIEW_FUNCTION,
        buildJsonObject { put("target_org_id", orgId) },
    ).decodeAs<Boolean>()

    override suspend fun list(orgId: String, status: String, before: ReportCursor?, limit: Int): List<ReportRowDto> =
        client.postgrest.rpc(
            LIST_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("status_filter", status)
                put("page_size", limit)
                if (before != null) {
                    put("before_created_at", before.createdAt)
                    put("before_report_id", before.id)
                }
            },
        ).decodeList()

    /** The answer carries more than the app reads, so it is decoded here and not with the shared settings. */
    override suspend fun detail(reportId: String): ReportDetailDto = json.decodeFromString(
        client.postgrest.rpc(GET_FUNCTION, buildJsonObject { put("target_report_id", reportId) }).data,
    )

    override suspend fun decide(request: ReportDecisionRequest) {
        client.postgrest.rpc(
            UPDATE_FUNCTION,
            buildJsonObject {
                put("target_report_id", request.reportId)
                put("new_status", request.status)
                put("action_note", request.note)
                put("remove_message", request.removeMessage)
                put("reset_username", request.resetUsername)
                put("remove_picture", request.removePicture)
                put("block_days", request.blockDays)
                put("rename_group_to", request.renameGroupTo)
            },
        )
    }

    override suspend fun revealReporter(reportId: String, reason: String): RevealedReporterDto = json.decodeFromString(
        client.postgrest.rpc(
            REVEAL_FUNCTION,
            buildJsonObject {
                put("target_report_id", reportId)
                put("reveal_reason", reason)
            },
        ).data,
    )

    override suspend fun reviewers(orgId: String): List<ReviewerDto> = client.postgrest.rpc(
        LIST_REVIEWERS_FUNCTION,
        buildJsonObject { put("target_org_id", orgId) },
    ).decodeList()

    override suspend fun setReviewers(orgId: String, userIds: List<String>) {
        client.postgrest.rpc(
            SET_REVIEWERS_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("reviewer_ids", JsonArray(userIds.map(::JsonPrimitive)))
            },
        )
    }
}
