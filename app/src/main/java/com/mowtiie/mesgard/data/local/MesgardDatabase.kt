package com.mowtiie.mesgard.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        ProfileEntity::class,
        ContactEntity::class,
        GroupEntity::class,
        AccountEntity::class,
        AccountMembershipEntity::class,
    ],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
    ],
)
internal abstract class MesgardDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    abstract fun conversationDao(): ConversationDao

    abstract fun profileDao(): ProfileDao

    abstract fun contactDao(): ContactDao

    abstract fun groupDao(): GroupDao

    abstract fun accountDao(): AccountDao

    companion object {
        const val NAME = "mesgard.db"
    }
}
