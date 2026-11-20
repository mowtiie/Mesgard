package com.mowtiie.mesgard.data.local.security

import java.io.File
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val LEGACY = "legacy"
private const val RAW = "raw"

@RunWith(RobolectricTestRunner::class)
class DatabaseKeyMigratorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val secret = ByteArray(32) { it.toByte() }
    private val rawKey = rawKeyOf(secret)
    private lateinit var database: File
    private val keyring = FakeKeyring()
    private val migrator = DatabaseKeyMigrator(keyring)

    @Before
    fun prepareFile() {
        database = File(temporaryFolder.root, "mesgard.db")
    }

    @Test
    fun `the raw key is the secret in SQLCipher raw key syntax`() {
        assertEquals("x'000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f'", String(rawKey))
        assertEquals(67, rawKey.size)
    }

    @Test
    fun `a new install uses the raw key and touches nothing`() {
        assertArrayEquals(rawKey, migrator.prepare(database, secret))

        assertEquals(emptyList<String>(), keyring.calls)
    }

    @Test
    fun `a database that already opens with the raw key is left alone`() {
        database.writeText(RAW)

        assertArrayEquals(rawKey, migrator.prepare(database, secret))

        assertEquals(listOf("opens raw"), keyring.calls)
        assertEquals(RAW, database.readText())
    }

    @Test
    fun `a database keyed with the passphrase is rekeyed to the raw key and the copy is removed`() {
        database.writeText(LEGACY)
        File(database.path + "-wal").writeText("wal")

        assertArrayEquals(rawKey, migrator.prepare(database, secret))

        assertEquals(listOf("opens raw", "opens secret", "rekey", "opens raw"), keyring.calls)
        assertEquals(RAW, database.readText())
        assertEquals(emptyList<String>(), backupNames())
    }

    @Test
    fun `a rekey that fails keeps the database and uses the old key until the next try`() {
        database.writeText(LEGACY)
        File(database.path + "-wal").writeText("wal")
        keyring.onRekey = {
            database.writeText("half written")
            throw IOException("disk full")
        }

        assertArrayEquals(secret, migrator.prepare(database, secret))

        assertEquals(LEGACY, database.readText())
        assertEquals("wal", File(database.path + "-wal").readText())
        assertEquals(emptyList<String>(), backupNames())
    }

    @Test
    fun `a rekey that leaves a database the raw key cannot open is undone`() {
        database.writeText(LEGACY)
        keyring.onRekey = { database.writeText("garbage") }

        assertArrayEquals(secret, migrator.prepare(database, secret))

        assertEquals(LEGACY, database.readText())
    }

    @Test
    fun `a database that opens with neither key is removed so it can be fetched again`() {
        database.writeText("unknown")
        File(database.path + "-wal").writeText("wal")

        assertArrayEquals(rawKey, migrator.prepare(database, secret))

        assertFalse(database.exists())
        assertFalse(File(database.path + "-wal").exists())
    }

    @Test
    fun `an interrupted migration that left a readable raw key database only removes the copy`() {
        database.writeText(RAW)
        File(database.path + ".key-backup").writeText(LEGACY)

        assertArrayEquals(rawKey, migrator.prepare(database, secret))

        assertEquals(RAW, database.readText())
        assertEquals(emptyList<String>(), backupNames())
    }

    @Test
    fun `an interrupted migration that left an unreadable database puts the earlier copy back and migrates it`() {
        database.writeText("half written")
        File(database.path + ".key-backup").writeText(LEGACY)

        assertArrayEquals(rawKey, migrator.prepare(database, secret))

        assertEquals(RAW, database.readText())
        assertEquals(emptyList<String>(), backupNames())
    }

    private fun backupNames() = temporaryFolder.root.list().orEmpty().filter { it.endsWith(".key-backup") }

    /** A database is a file whose text says which key opens it: "legacy" the secret, "raw" the raw key. */
    private inner class FakeKeyring : DatabaseKeyring {
        val calls = mutableListOf<String>()
        var onRekey: () -> Unit = { database.writeText(RAW) }

        override fun opens(database: File, key: ByteArray): Boolean {
            val isRaw = key.contentEquals(rawKey)
            calls += if (isRaw) "opens raw" else "opens secret"
            if (!database.exists()) return false
            return database.readText() == if (isRaw) RAW else LEGACY
        }

        override fun rekey(database: File, currentKey: ByteArray, newKey: ByteArray): Boolean {
            calls += "rekey"
            return try {
                onRekey()
                true
            } catch (_: IOException) {
                false
            }
        }
    }
}
