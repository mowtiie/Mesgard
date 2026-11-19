package com.mowtiie.mesgard.data.local

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/** The server turned announcement conversations into ordinary groups, so the stored ones become groups too. */
internal class AnnouncementsBecomeGroups : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE conversations SET type = 'GROUP' WHERE type = 'ANNOUNCEMENT'")
    }
}
