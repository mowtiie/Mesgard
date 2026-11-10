package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Upsert

@Dao
internal interface ProfileDao {
    @Upsert
    suspend fun upsertAll(profiles: List<ProfileEntity>)
}
