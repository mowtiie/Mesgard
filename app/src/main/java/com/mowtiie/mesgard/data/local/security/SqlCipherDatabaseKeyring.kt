package com.mowtiie.mesgard.data.local.security

import android.database.sqlite.SQLiteException
import java.io.File
import net.zetetic.database.sqlcipher.SQLiteDatabase

internal class SqlCipherDatabaseKeyring : DatabaseKeyring {
    override fun opens(database: File, key: ByteArray): Boolean = try {
        open(database, key).use { true }
    } catch (_: SQLiteException) {
        false
    }

    override fun rekey(database: File, currentKey: ByteArray, newKey: ByteArray): Boolean = try {
        open(database, currentKey).use { it.changePassword(newKey) }
        true
    } catch (_: SQLiteException) {
        false
    }

    private fun open(database: File, key: ByteArray): SQLiteDatabase =
        SQLiteDatabase.openDatabase(database.path, key, null, SQLiteDatabase.OPEN_READWRITE, null, null)
}
