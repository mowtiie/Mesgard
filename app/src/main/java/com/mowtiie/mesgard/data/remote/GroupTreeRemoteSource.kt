package com.mowtiie.mesgard.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

private const val GROUPS_TABLE = "groups"

internal interface GroupTreeRemoteSource {
    /** Both return true when a row changed; Row Level Security hides the groups a person may not change. */
    suspend fun setKind(groupId: String, kind: String): Boolean

    suspend fun setParent(groupId: String, parentId: String?): Boolean
}

internal class SupabaseGroupTreeRemoteSource @Inject constructor(private val client: SupabaseClient) :
    GroupTreeRemoteSource {
    override suspend fun setKind(groupId: String, kind: String): Boolean =
        client.from(GROUPS_TABLE).update({ set("kind", kind) }) {
            select(Columns.list("id"))
            filter { eq("id", groupId) }
        }.decodeList<IdDto>().isNotEmpty()

    override suspend fun setParent(groupId: String, parentId: String?): Boolean =
        client.from(GROUPS_TABLE).update({ set("parent_id", parentId) }) {
            select(Columns.list("id"))
            filter { eq("id", groupId) }
        }.decodeList<IdDto>().isNotEmpty()
}
