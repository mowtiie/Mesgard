package com.mowtiie.mesgard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
internal data class GroupEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val name: String,
    @ColumnInfo(name = "can_start_conversation") val canStartConversation: Boolean,
)
