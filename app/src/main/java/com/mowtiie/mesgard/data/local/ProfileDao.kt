package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mowtiie.mesgard.domain.model.MentionCandidate

@Dao
internal interface ProfileDao {
    @Upsert
    suspend fun upsertAll(profiles: List<ProfileEntity>)

    @Query("SELECT * FROM profiles WHERE id IN (:userIds)")
    suspend fun findDisplayNames(userIds: List<String>): List<ProfileEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM profiles WHERE id = :userId)")
    suspend fun exists(userId: String): Boolean

    /** People who wrote in the conversation and whose name contains [query], so mentions work without a connection. */
    @Query(
        """
        SELECT DISTINCT p.id AS userId, p.display_name AS displayName
        FROM profiles AS p
        JOIN messages AS m ON m.sender_id = p.id
        WHERE m.conversation_id = :conversationId AND p.id <> :excludedUserId
            AND instr(lower(p.display_name), lower(:query)) > 0
        ORDER BY p.display_name COLLATE NOCASE ASC, p.id ASC
        LIMIT :limit
        """,
    )
    suspend fun findSenders(
        conversationId: String,
        query: String,
        excludedUserId: String,
        limit: Int,
    ): List<MentionCandidate>

    @Query("SELECT display_name FROM profiles WHERE id = :userId")
    suspend fun findDisplayName(userId: String): String?
}
