package com.mowtiie.mesgard.data.local.security

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabasePassphraseProviderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val storageFile: File by lazy { File(temporaryFolder.root, "database.key") }

    @Test
    fun `creates a 32 byte passphrase when none is stored`() {
        val passphrase = DatabasePassphraseProvider(storageFile, ReversingCipher).getOrCreate()

        assertEquals(EXPECTED_PASSPHRASE_BYTES, passphrase.size)
    }

    @Test
    fun `returns the same passphrase to a later provider using the same storage`() {
        val created = DatabasePassphraseProvider(storageFile, ReversingCipher).getOrCreate()

        val restored = DatabasePassphraseProvider(storageFile, ReversingCipher).getOrCreate()

        assertArrayEquals(created, restored)
    }

    @Test
    fun `stores the passphrase only in encrypted form`() {
        val passphrase = DatabasePassphraseProvider(storageFile, ReversingCipher).getOrCreate()

        assertFalse(storageFile.readBytes().contentEquals(passphrase))
    }

    @Test
    fun `generates a different passphrase for each installation`() {
        val first = DatabasePassphraseProvider(File(temporaryFolder.root, "first.key"), ReversingCipher).getOrCreate()
        val second = DatabasePassphraseProvider(File(temporaryFolder.root, "second.key"), ReversingCipher).getOrCreate()

        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `does not write storage when the cipher fails`() {
        val provider = DatabasePassphraseProvider(storageFile, FailingCipher)

        val result = runCatching { provider.getOrCreate() }

        assertTrue(result.isFailure)
        assertFalse(storageFile.exists())
    }

    private object ReversingCipher : SecretCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = plaintext.reversedArray()

        override fun decrypt(payload: ByteArray): ByteArray = payload.reversedArray()
    }

    private object FailingCipher : SecretCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = error("Cipher unavailable")

        override fun decrypt(payload: ByteArray): ByteArray = error("Cipher unavailable")
    }

    private companion object {
        const val EXPECTED_PASSPHRASE_BYTES = 32
    }
}
