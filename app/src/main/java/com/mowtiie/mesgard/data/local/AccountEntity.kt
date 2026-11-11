package com.mowtiie.mesgard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account")
internal data class AccountEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val email: String?,
)

@Entity(tableName = "account_memberships")
internal data class AccountMembershipEntity(
    @PrimaryKey @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "org_name") val orgName: String,
    val role: String,
)
