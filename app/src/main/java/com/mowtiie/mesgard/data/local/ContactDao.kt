package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY display_name COLLATE NOCASE ASC, user_id ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Upsert
    suspend fun upsertAll(contacts: List<ContactEntity>)

    /** The names of some people as profiles, whichever organization the roster entry is from. */
    @Query("SELECT user_id AS id, display_name FROM contacts WHERE user_id IN (:userIds)")
    suspend fun findDisplayNames(userIds: List<String>): List<ProfileEntity>

    @Query("UPDATE contacts SET is_favorite = :isFavorite WHERE org_id = :orgId AND user_id IN (:userIds)")
    suspend fun updateFavorite(orgId: String, userIds: List<String>, isFavorite: Boolean)

    @Query("SELECT user_id FROM contacts WHERE is_favorite = 1")
    suspend fun findFavoriteIds(): List<String>

    @Query("SELECT COUNT(*) FROM contacts WHERE is_favorite = 1")
    suspend fun countFavorites(): Int

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}
