package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DirectPeerDao {
    @Query("SELECT conversation_id FROM direct_peers WHERE conversation_id IN (:conversationIds)")
    suspend fun findKnownConversationIds(conversationIds: List<String>): List<String>

    /** The direct conversation with a person in an organization, when this phone already knows it. */
    @Query(
        """
        SELECT peer.conversation_id FROM direct_peers AS peer
        JOIN conversations AS conversation ON conversation.id = peer.conversation_id
        WHERE peer.user_id = :userId AND conversation.org_id = :orgId
        LIMIT 1
        """,
    )
    suspend fun findConversationWith(userId: String, orgId: String): String?

    /** The people the user chatted with most recently, who are on the roster, newest conversation first. */
    @Query(
        """
        SELECT contact.* FROM direct_peers AS peer
        JOIN conversations AS conversation ON conversation.id = peer.conversation_id
        JOIN contacts AS contact ON contact.user_id = peer.user_id AND contact.org_id = conversation.org_id
        WHERE conversation.archived_at IS NULL
        ORDER BY COALESCE(
            (SELECT MAX(created_at) FROM messages WHERE conversation_id = conversation.id),
            conversation.created_at
        ) DESC
        LIMIT :limit
        """,
    )
    fun observeRecentContacts(limit: Int): Flow<List<ContactEntity>>

    @Upsert
    suspend fun upsertAll(peers: List<DirectPeerEntity>)

    /** The direct conversations with a person who is a favorite. */
    @Query(
        """
        SELECT DISTINCT peer.conversation_id FROM direct_peers AS peer
        JOIN contacts AS contact ON contact.user_id = peer.user_id
        WHERE contact.is_favorite = 1
        """,
    )
    fun observeFavoriteConversationIds(): Flow<List<String>>
}
