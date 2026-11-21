package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PresenceAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeJoinsAs
import io.github.jan.supabase.realtime.decodeLeavesAs
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.track
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TOPIC_PREFIX = "org:"
private const val MEMBERS_TABLE = "org_members"

@Serializable
internal data class PresencePayload(@SerialName("user_id") val userId: String)

/** An organization the person belongs to, and whether they are a student there, which decides who is shown online. */
internal data class PresenceMembership(val organizationId: String, val isStudent: Boolean)

internal interface PresenceRemoteSource {
    suspend fun fetchMemberships(userId: String): List<PresenceMembership>

    /**
     * Joins the private presence channel of the organization and emits the identifiers of all users that are online in
     * the organization every time that set changes. [userId] is announced as online only when [announce] is true, so a
     * person who hides their status still sees who is online. The user goes offline when the collection ends.
     */
    fun session(organizationId: String, userId: String, announce: Boolean): Flow<Set<String>>
}

internal class SupabasePresenceRemoteSource @Inject constructor(private val client: SupabaseClient) :
    PresenceRemoteSource {
    override suspend fun fetchMemberships(userId: String): List<PresenceMembership> =
        client.from(MEMBERS_TABLE).select(Columns.list("org_id", "role")) {
            filter { eq("user_id", userId) }
        }.decodeList<MembershipDto>().map { PresenceMembership(it.orgId, isStudent = it.role == "member") }

    override fun session(organizationId: String, userId: String, announce: Boolean): Flow<Set<String>> = channelFlow {
        val channel = client.channel(TOPIC_PREFIX + organizationId) { isPrivate = true }
        val changes = channel.presenceChangeFlow()
        try {
            channel.subscribe(blockUntilSubscribed = true)
            if (announce) channel.track(PresencePayload(userId))
            var online = emptySet<String>()
            changes.collect { change ->
                online = online.applying(change)
                send(online)
            }
        } finally {
            withContext(NonCancellable) { client.realtime.removeChannel(channel) }
        }
    }
}

private fun Set<String>.applying(change: PresenceAction): Set<String> =
    this - change.decodeLeavesAs<PresencePayload>().map { it.userId }.toSet() +
        change.decodeJoinsAs<PresencePayload>().map { it.userId }
