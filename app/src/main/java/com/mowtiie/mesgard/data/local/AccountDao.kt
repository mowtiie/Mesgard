package com.mowtiie.mesgard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AccountDao {
    @Query("SELECT * FROM account LIMIT 1")
    fun observeAccount(): Flow<AccountEntity?>

    @Query("SELECT * FROM account_memberships ORDER BY org_name COLLATE NOCASE ASC, org_id ASC")
    fun observeMemberships(): Flow<List<AccountMembershipEntity>>

    @Upsert
    suspend fun upsertAccount(account: AccountEntity)

    @Upsert
    suspend fun upsertMemberships(memberships: List<AccountMembershipEntity>)

    @Query("DELETE FROM account")
    suspend fun deleteAccounts()

    @Query("DELETE FROM account_memberships")
    suspend fun deleteMemberships()
}
