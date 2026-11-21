package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val GET_FUNCTION = "get_messaging_rules"
private const val SET_FUNCTION = "set_messaging_rules"

@Serializable
internal data class MessagingRulesDto(
    @SerialName("block_staff_student_direct_messages") val blockStaffStudentDirectMessages: Boolean,
    @SerialName("quiet_hours_start") val quietHoursStart: String? = null,
    @SerialName("quiet_hours_end") val quietHoursEnd: String? = null,
    @SerialName("quiet_hours_timezone") val quietHoursTimeZone: String,
)

internal data class SetMessagingRulesRequest(
    val orgId: String,
    val blockStaffStudent: Boolean,
    val quietStart: String?,
    val quietEnd: String?,
    val timeZone: String,
)

internal interface MessagingRulesRemoteSource {
    suspend fun rules(orgId: String): MessagingRulesDto

    suspend fun setRules(request: SetMessagingRulesRequest)
}

internal class SupabaseMessagingRulesRemoteSource @Inject constructor(private val client: SupabaseClient) :
    MessagingRulesRemoteSource {
    override suspend fun rules(orgId: String): MessagingRulesDto = client.postgrest.rpc(
        GET_FUNCTION,
        buildJsonObject { put("target_org_id", orgId) },
    ).decodeList<MessagingRulesDto>().first()

    override suspend fun setRules(request: SetMessagingRulesRequest) {
        client.postgrest.rpc(
            SET_FUNCTION,
            buildJsonObject {
                put("target_org_id", request.orgId)
                put("block_staff_student", request.blockStaffStudent)
                put("quiet_start", request.quietStart)
                put("quiet_end", request.quietEnd)
                put("quiet_timezone", request.timeZone)
            },
        )
    }
}
