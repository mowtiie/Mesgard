package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DirectPeerDao {
    @Query("SELECT conversation_id FROM direct_peers WHERE conversation_id IN (:conversationIds)")
    suspend fun findKnownConversationIds(conversationIds: List<String>): List<String>

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
