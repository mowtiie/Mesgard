package com.mowtiie.mesgard.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Opens the database as it was in every earlier version, from the exported schemas, and migrates it to the newest one.
 * Room checks that the result has exactly the tables, columns and indices of the newest schema, so a migration that
 * would crash the app on update fails here instead.
 */
@RunWith(RobolectricTestRunner::class)
class MesgardDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MesgardDatabase::class.java)

    private val latestVersion: Int by lazy {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        checkNotNull(assets.list(MesgardDatabase::class.java.name)).maxOf { it.removeSuffix(".json").toInt() }
    }

    @Test
    fun `every exported version is a version of the database up to the newest one`() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val versions = checkNotNull(assets.list(MesgardDatabase::class.java.name))
            .map { it.removeSuffix(".json").toInt() }
            .sorted()

        assertEquals((1..latestVersion).toList(), versions)
        assertTrue("There are no earlier versions to migrate", latestVersion > 1)
    }

    @Test
    fun `an empty database of every earlier version migrates to the newest version`() {
        for (version in 1 until latestVersion) {
            val name = databasePath("migration-from-$version")

            helper.createDatabase(name, version).close()
            helper.runMigrationsAndValidate(name, latestVersion, true).close()
        }
    }

    @Test
    fun `a message stored by the first version survives every migration`() {
        val name = databasePath("message-from-version-1")
        helper.createDatabase(name, 1).apply {
            insert(
                "messages",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", "m1")
                    put("conversation_id", "c1")
                    put("sender_id", "u1")
                    put("body", "Hello from version 1")
                    put("created_at", CREATED_AT)
                    put("status", "SENT")
                },
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(name, latestVersion, true)

        migrated.query("SELECT body, created_at, status FROM messages WHERE id = 'm1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Hello from version 1", cursor.getString(0))
            assertEquals(CREATED_AT, cursor.getLong(1))
            assertEquals("SENT", cursor.getString(2))
        }
        migrated.close()
    }

    /** The helper opens the file by its full path, so the name it is given must be the full path as well. */
    private fun databasePath(name: String): String =
        ApplicationProvider.getApplicationContext<Context>().getDatabasePath(name).absolutePath

    private companion object {
        const val CREATED_AT = 1_700_000_000_000L
    }
}
