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
        AttachmentEntity::class,
        PinnedMessageEntity::class,
        DirectPeerEntity::class,
    ],
    version = 19,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15, spec = AnnouncementsBecomeGroups::class),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
    ],
)
internal abstract class MesgardDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    abstract fun conversationDao(): ConversationDao

    abstract fun conversationPreferencesDao(): ConversationPreferencesDao

    abstract fun profileDao(): ProfileDao

    abstract fun contactDao(): ContactDao

    abstract fun groupDao(): GroupDao

    abstract fun accountDao(): AccountDao

    abstract fun attachmentDao(): AttachmentDao

    abstract fun pinnedMessageDao(): PinnedMessageDao

    abstract fun directPeerDao(): DirectPeerDao

    companion object {
        const val NAME = "mesgard.db"
    }
}
