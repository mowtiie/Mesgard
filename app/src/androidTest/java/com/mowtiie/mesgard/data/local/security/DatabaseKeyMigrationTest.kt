package com.mowtiie.mesgard.data.local.security

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.security.SecureRandom
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Uses throwaway files in the cache directory, never the database of the app. */
@RunWith(AndroidJUnit4::class)
class DatabaseKeyMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val directory = File(context.cacheDir, "key-migration-test")
    private val database = File(directory, "migrating.db")
    private val secret = ByteArray(SECRET_BYTES).also(SecureRandom()::nextBytes)
    private val keyring = SqlCipherDatabaseKeyring()

    @Before
    fun prepare() {
        System.loadLibrary("sqlcipher")
        directory.deleteRecursively()
        directory.mkdirs()
    }

    @After
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun aDatabaseKeyedWithThePassphraseMovesToTheRawKeyAndKeepsItsRows() {
        createLegacyDatabase()

        val key = DatabaseKeyMigrator(keyring).prepare(database, secret)

        assertArrayEquals(rawKeyOf(secret), key)
        assertTrue(keyring.opens(database, rawKeyOf(secret)))
        assertFalse(keyring.opens(database, secret))
        assertEquals(ROW_COUNT, countRows(rawKeyOf(secret)))
        assertEquals(emptyList<String>(), directory.list().orEmpty().filter { it.endsWith(".key-backup") })
    }

    @Test
    fun aDatabaseWithRowsStillInItsWriteAheadLogMovesToTheRawKeyAndKeepsThem() {
        createLegacyDatabaseWithUncheckpointedFrames()

        val key = DatabaseKeyMigrator(keyring).prepare(database, secret)

        assertArrayEquals(rawKeyOf(secret), key)
        assertEquals(ROW_COUNT, countRows(rawKeyOf(secret)))
    }

    @Test
    fun aMigratedDatabaseOpensWithoutKeyDerivationAndIsLeftAlone() {
        createLegacyDatabase()
        DatabaseKeyMigrator(keyring).prepare(database, secret)

        val start = SystemClock.elapsedRealtime()
        val key = DatabaseKeyMigrator(keyring).prepare(database, secret)
        val elapsed = SystemClock.elapsedRealtime() - start

        assertArrayEquals(rawKeyOf(secret), key)
        assertTrue("Opening with the raw key took $elapsed ms", elapsed < QUICK_OPEN_MS)
        assertEquals(ROW_COUNT, countRows(rawKeyOf(secret)))
    }

    @Test
    fun aDatabaseThatOpensWithNeitherKeyIsRemoved() {
        database.writeText("not a database")

        val key = DatabaseKeyMigrator(keyring).prepare(database, secret)

        assertArrayEquals(rawKeyOf(secret), key)
        assertFalse(database.exists())
    }

    private fun createLegacyDatabase() {
        SQLiteDatabase.openOrCreateDatabase(database.path, secret, null, null, null).use { db ->
            db.enableWriteAheadLogging()
            db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, body TEXT)")
            repeat(ROW_COUNT) { db.execSQL("INSERT INTO messages (body) VALUES ('message $it')") }
        }
    }

    /** Copies the files while the writer is still open, as they are after the app was killed. */
    private fun createLegacyDatabaseWithUncheckpointedFrames() {
        val original = File(directory, "original.db")
        SQLiteDatabase.openOrCreateDatabase(original.path, secret, null, null, null).use { db ->
            db.enableWriteAheadLogging()
            db.rawQuery("PRAGMA wal_autocheckpoint = 0", null).use { it.moveToFirst() }
            db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, body TEXT)")
            repeat(ROW_COUNT) { db.execSQL("INSERT INTO messages (body) VALUES ('message $it')") }
            original.copyTo(database)
            File(original.path + "-wal").copyTo(File(database.path + "-wal"))
        }
        assertTrue("The copied log has no frames", File(database.path + "-wal").length() > 0)
    }

    private fun countRows(key: ByteArray): Int =
        SQLiteDatabase.openDatabase(database.path, key, null, SQLiteDatabase.OPEN_READWRITE, null, null).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM messages", null).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
        }

    private companion object {
        const val SECRET_BYTES = 32
        const val ROW_COUNT = 50
        const val QUICK_OPEN_MS = 150L
    }
}
