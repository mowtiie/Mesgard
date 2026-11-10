package com.mowtiie.mesgard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class], version = 1)
internal abstract class MesgardDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        const val NAME = "mesgard.db"
    }
}
