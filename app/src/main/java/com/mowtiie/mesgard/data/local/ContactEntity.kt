package com.mowtiie.mesgard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "contacts", primaryKeys = ["org_id", "user_id"])
internal data class ContactEntity(
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val role: String,
)
