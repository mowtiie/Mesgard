package com.mowtiie.mesgard.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.mesgard.data.local.security.DatabasePassphraseProvider
import com.mowtiie.mesgard.data.local.security.KeystoreSecretCipher
import com.mowtiie.mesgard.domain.model.MessageStatus
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val passphraseFile = File(context.noBackupFilesDir, "test-database.key")

    @Before
    fun prepare() {
        System.loadLibrary("sqlcipher")
        cleanUp()
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(DATABASE_NAME)
        passphraseFile.delete()
    }

    @Test
    fun keystoreCipherDecryptsWhatItEncrypted() {
        val cipher = KeystoreSecretCipher()
        val secret = ByteArray(SECRET_BYTES) { it.toByte() }

        val restored = cipher.decrypt(cipher.encrypt(secret))

        assertArrayEquals(secret, restored)
    }

    @Test
    fun databaseFileIsNotReadableAsPlainSqlite() {
        storeMessage(passphraseProvider().getOrCreate())

        val header = ByteArray(PLAIN_HEADER.length)
        context.getDatabasePath(DATABASE_NAME).inputStream().use { it.read(header) }

        assertFalse(String(header, Charsets.ISO_8859_1) == PLAIN_HEADER)
    }

    @Test
    fun databaseReopensWithTheStoredPassphrase() {
        storeMessage(passphraseProvider().getOrCreate())

        val stored = readMessages(passphraseProvider().getOrCreate())

        assertEquals(listOf(MESSAGE_ID), stored.map { it.id })
    }

    @Test
    fun databaseCannotBeOpenedWithAnotherPassphrase() {
        storeMessage(passphraseProvider().getOrCreate())
        val wrongPassphrase = ByteArray(PASSPHRASE_BYTES) { WRONG_BYTE }

        val result = runCatching { readMessages(wrongPassphrase) }

        assertTrue(result.isFailure)
    }

    private fun passphraseProvider() = DatabasePassphraseProvider(passphraseFile, KeystoreSecretCipher())

    private fun storeMessage(passphrase: ByteArray) = withDatabase(passphrase) { database ->
        database.messageDao().upsert(message())
    }

    private fun readMessages(passphrase: ByteArray) = withDatabase(passphrase) { database ->
        database.messageDao().observeLatest(CONVERSATION_ID, PAGE_SIZE).first()
    }

    private fun <T> withDatabase(passphrase: ByteArray, block: suspend (MesgardDatabase) -> T): T {
        val database = Room.databaseBuilder(context, MesgardDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
        try {
            return runBlocking { block(database) }
        } finally {
            database.close()
        }
    }

    private fun message() = MessageEntity(
        id = MESSAGE_ID,
        conversationId = CONVERSATION_ID,
        senderId = "sender",
        body = "Hello",
        createdAtEpochMillis = 1_000L,
        status = MessageStatus.SENT,
    )

    private companion object {
        const val DATABASE_NAME = "encrypted-test.db"
        const val PLAIN_HEADER = "SQLite format 3\u0000"
        const val MESSAGE_ID = "message"
        const val CONVERSATION_ID = "conversation"
        const val SECRET_BYTES = 32
        const val PASSPHRASE_BYTES = 32
        const val WRONG_BYTE: Byte = 7
        const val PAGE_SIZE = 10
    }
}
