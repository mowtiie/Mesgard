package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PARTICIPANTS_TABLE = "conversation_participants"
private const val NAME_PATH = "org_members.profiles.display_name"

@Serializable
internal data class ParticipantNameDto(
    @SerialName("user_id") val userId: String,
    @SerialName("org_members") val member: MemberNameDto,
)

@Serializable
internal data class MemberNameDto(val profiles: ReaderProfileDto)

internal interface MentionRemoteSource {
    suspend fun searchParticipants(conversationId: String, query: String, limit: Int): List<ParticipantNameDto>
}

internal class SupabaseMentionRemoteSource @Inject constructor(private val client: SupabaseClient) :
    MentionRemoteSource {
    override suspend fun searchParticipants(
        conversationId: String,
        query: String,
        limit: Int,
    ): List<ParticipantNameDto> = client.from(
        PARTICIPANTS_TABLE,
    ).select(Columns.raw("user_id, org_members!inner(profiles!inner(display_name))")) {
        filter {
            eq("conversation_id", conversationId)
            filter(NAME_PATH, FilterOperator.ILIKE, "*$query*")
        }
        limit(limit.toLong())
    }.decodeList<ParticipantNameDto>()
}
