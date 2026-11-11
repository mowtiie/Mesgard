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

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}
