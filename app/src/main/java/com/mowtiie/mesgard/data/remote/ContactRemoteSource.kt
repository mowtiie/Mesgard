package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.domain.repository.ROSTER_LIMIT
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val MEMBERS_TABLE = "org_members"
private const val CREATE_DIRECT_FUNCTION = "create_direct_conversation"
private const val FAVORITES_TABLE = "favorite_contacts"
internal const val SEARCH_RESULT_LIMIT = 20
private val SEARCH_COLUMNS =
    Columns.raw("org_id, user_id, role, user:profiles!org_members_user_id_fkey!inner(id, display_name)")
private val CONTACT_COLUMNS =
    Columns.raw("org_id, user_id, role, user:profiles!org_members_user_id_fkey(id, display_name)")

internal interface ContactRemoteSource {
    suspend fun fetchContacts(excludingUserId: String?): List<ContactDto>

    suspend fun createDirectConversation(orgId: String, otherUserId: String): String

    /** Up to [SEARCH_RESULT_LIMIT] people whose name contains the text, whatever the size of the roster. */
    suspend fun searchContacts(query: String, excludingUserId: String?): List<ContactDto>

    suspend fun fetchContactsByIds(userIds: Collection<String>): List<ContactDto>

    /** The people the signed in user keeps as favorites. */
    suspend fun fetchFavoriteIds(userId: String?): Set<String>

    suspend fun addFavorites(orgId: String, userId: String, favoriteIds: Collection<String>)

    suspend fun removeFavorites(orgId: String, userId: String, favoriteIds: Collection<String>)
}

internal class SupabaseContactRemoteSource @Inject constructor(private val client: SupabaseClient) :
    ContactRemoteSource {
    override suspend fun fetchContacts(excludingUserId: String?): List<ContactDto> =
        client.from(MEMBERS_TABLE).select(CONTACT_COLUMNS) {
            filter { if (excludingUserId != null) neq("user_id", excludingUserId) }
            order("created_at", Order.ASCENDING)
            limit(ROSTER_LIMIT.toLong())
        }.decodeList<ContactDto>()

    override suspend fun searchContacts(query: String, excludingUserId: String?): List<ContactDto> =
        client.from(MEMBERS_TABLE).select(SEARCH_COLUMNS) {
            filter {
                ilike("user.display_name", "%${escapeForLike(query)}%")
                if (excludingUserId != null) neq("user_id", excludingUserId)
            }
            order("created_at", Order.ASCENDING)
            limit(SEARCH_RESULT_LIMIT.toLong())
        }.decodeList<ContactDto>()

    override suspend fun fetchContactsByIds(userIds: Collection<String>): List<ContactDto> {
        if (userIds.isEmpty()) return emptyList()
        return client.from(MEMBERS_TABLE).select(CONTACT_COLUMNS) {
            filter { isIn("user_id", userIds.toList()) }
        }.decodeList<ContactDto>()
    }

    override suspend fun fetchFavoriteIds(userId: String?): Set<String> {
        if (userId == null) return emptySet()
        return client.from(FAVORITES_TABLE).select(Columns.list("favorite_user_id")) {
            filter { eq("user_id", userId) }
        }.decodeList<FavoriteDto>().map { it.favoriteUserId }.toSet()
    }

    override suspend fun addFavorites(orgId: String, userId: String, favoriteIds: Collection<String>) {
        val rows = favoriteIds.map { FavoriteRow(orgId, userId, it) }
        client.from(FAVORITES_TABLE).upsert(rows) {
            onConflict = "org_id,user_id,favorite_user_id"
            ignoreDuplicates = true
        }
    }

    override suspend fun removeFavorites(orgId: String, userId: String, favoriteIds: Collection<String>) {
        client.from(FAVORITES_TABLE).delete {
            filter {
                eq("org_id", orgId)
                eq("user_id", userId)
                isIn("favorite_user_id", favoriteIds.toList())
            }
        }
    }

    override suspend fun createDirectConversation(orgId: String, otherUserId: String): String = client.postgrest
        .rpc(
            CREATE_DIRECT_FUNCTION,
            buildJsonObject {
                put("target_org_id", orgId)
                put("other_user_id", otherUserId)
            },
        )
        .decodeAs<String>()
}

@Serializable
private data class FavoriteDto(@SerialName("favorite_user_id") val favoriteUserId: String)

@Serializable
private data class FavoriteRow(
    @SerialName("org_id") val orgId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("favorite_user_id") val favoriteUserId: String,
)

/** The characters that mean something in a LIKE pattern or in the request are taken out or escaped. */
internal fun escapeForLike(text: String): String = text.trim()
    .replace(Regex("[%*,()\\\\]"), "")
    .replace("_", "\\_")
