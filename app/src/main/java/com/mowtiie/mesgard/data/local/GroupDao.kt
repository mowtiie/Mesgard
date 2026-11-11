package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface GroupDao {
    @Query("SELECT * FROM `groups` ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<GroupEntity>>

    @Upsert
    suspend fun upsertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM `groups`")
    suspend fun deleteAll()
}
