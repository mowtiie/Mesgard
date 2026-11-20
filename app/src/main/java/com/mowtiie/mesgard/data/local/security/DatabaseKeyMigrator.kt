package com.mowtiie.mesgard.data.local.security

import android.util.Log
import java.io.File
import java.io.IOException

private const val TAG = "DatabaseKeyMigrator"
private const val BACKUP_SUFFIX = ".key-backup"
private val DATABASE_FILE_SUFFIXES = listOf("", "-wal", "-shm", "-journal")

/** What the migrator needs from SQLCipher, so the decisions can be tested without the native library. */
internal interface DatabaseKeyring {
    /** True when the database exists and is readable with [key]. Never throws for a wrong key. */
    fun opens(database: File, key: ByteArray): Boolean

    /** True when the database was re-encrypted with [newKey]. False when SQLCipher refused or failed. */
    fun rekey(database: File, currentKey: ByteArray, newKey: ByteArray): Boolean
}

/**
 * The key as SQLCipher's raw key syntax, \`x'<64 hex digits>'\`. A raw key skips the 256,000 round key derivation that
 * a passphrase gets on every connection (about 0.5 s each on a phone). The secret is random, so nothing is lost.
 */
internal fun rawKeyOf(secret: ByteArray): ByteArray =
    ("x'" + secret.joinToString("") { "%02x".format(it) } + "'").toByteArray(Charsets.US_ASCII)

/**
 * Moves a database that was keyed with the secret as a passphrase to the raw key, once. Nothing is stored to say it
 * happened: the database is tried with the raw key first, which is quick, so a finished migration is recognized by
 * the database itself.
 */
internal class DatabaseKeyMigrator(private val keyring: DatabaseKeyring) {
    /** Returns the key to open [database] with. It is the raw key unless the migration failed and can be retried. */
    fun prepare(database: File, secret: ByteArray): ByteArray {
        val rawKey = rawKeyOf(secret)
        finishInterruptedMigration(database, rawKey)
        return when {
            !database.exists() || keyring.opens(database, rawKey) -> rawKey
            keyring.opens(database, secret) -> migrate(database, secret, rawKey)
            else -> {
                Log.w(TAG, "The local database opens with neither key, so it is removed and fetched again")
                delete(database)
                rawKey
            }
        }
    }

    private fun migrate(database: File, secret: ByteArray, rawKey: ByteArray): ByteArray {
        val migrated = try {
            copyAside(database)
            keyring.rekey(database, secret, rawKey) && keyring.opens(database, rawKey)
        } catch (exception: IOException) {
            Log.w(TAG, "The database could not be moved to the raw key", exception)
            false
        }
        if (migrated) {
            deleteBackup(database)
            return rawKey
        }
        restoreFromBackup(database)
        return secret
    }

    private fun finishInterruptedMigration(database: File, rawKey: ByteArray) {
        if (!backupOf(database, "").exists()) return
        if (database.exists() && keyring.opens(database, rawKey)) {
            deleteBackup(database)
        } else {
            Log.w(TAG, "A migration of the local database was interrupted, so the earlier copy is put back")
            restoreFromBackup(database)
        }
    }

    private fun copyAside(database: File) = DATABASE_FILE_SUFFIXES.forEach { suffix ->
        val source = fileOf(database, suffix)
        if (source.exists()) source.copyTo(backupOf(database, suffix), overwrite = true)
    }

    private fun restoreFromBackup(database: File) {
        DATABASE_FILE_SUFFIXES.forEach { suffix ->
            val backup = backupOf(database, suffix)
            val target = fileOf(database, suffix)
            if (backup.exists()) backup.copyTo(target, overwrite = true) else target.delete()
        }
        deleteBackup(database)
    }

    private fun deleteBackup(database: File) = DATABASE_FILE_SUFFIXES.forEach { backupOf(database, it).delete() }

    private fun delete(database: File) {
        DATABASE_FILE_SUFFIXES.forEach { fileOf(database, it).delete() }
        deleteBackup(database)
    }

    private fun fileOf(database: File, suffix: String) = File(database.path + suffix)

    private fun backupOf(database: File, suffix: String) = File(database.path + suffix + BACKUP_SUFFIX)
}
