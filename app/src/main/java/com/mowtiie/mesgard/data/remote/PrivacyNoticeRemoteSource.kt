package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LIST_FUNCTION = "list_privacy_notices"
private const val ACKNOWLEDGE_FUNCTION = "acknowledge_privacy_notice"
private const val SET_FUNCTION = "set_privacy_notice"

@Serializable
internal data class PrivacyNoticeDto(
    @SerialName("org_id") val orgId: String,
    @SerialName("org_name") val orgName: String,
    @SerialName("notice_url") val noticeUrl: String? = null,
    @SerialName("child_notice_url") val childNoticeUrl: String? = null,
    @SerialName("is_acknowledged") val isAcknowledged: Boolean,
)

internal interface PrivacyNoticeRemoteSource {
    suspend fun notices(): List<PrivacyNoticeDto>

    suspend fun acknowledge(orgId: String)

    suspend fun setLinks(orgId: String, noticeUrl: String?, childNoticeUrl: String?)
}

internal class SupabasePrivacyNoticeRemoteSource @Inject constructor(private val client: SupabaseClient) :
    PrivacyNoticeRemoteSource {
    override suspend fun notices(): List<PrivacyNoticeDto> = client.postgrest.rpc(LIST_FUNCTION).decodeList()

    override suspend fun acknowledge(orgId: String) {
        client.postgrest.rpc(ACKNOWLEDGE_FUNCTION, buildJsonObject { put("target_org_id", orgId) })
    }

    override suspend fun setLinks(orgId: String, noticeUrl: String?, childNoticeUrl: String?) {
        client.postgrest.rpc(
            SET_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("new_notice_url", noticeUrl)
                put("new_child_notice_url", childNoticeUrl)
            },
        )
    }
}
