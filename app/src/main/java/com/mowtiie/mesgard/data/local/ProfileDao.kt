package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
internal interface ProfileDao {
    @Upsert
    suspend fun upsertAll(profiles: List<ProfileEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM profiles WHERE id = :userId)")
    suspend fun exists(userId: String): Boolean

    @Query("SELECT display_name FROM profiles WHERE id = :userId")
    suspend fun findDisplayName(userId: String): String?
}
