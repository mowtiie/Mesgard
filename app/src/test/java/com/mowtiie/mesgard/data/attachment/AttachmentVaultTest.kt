package com.mowtiie.mesgard.data.attachment

import android.content.Context
import com.mowtiie.mesgard.data.local.security.SecretCipher
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Stands in for the Android Keystore: it hides the data key by flipping its bits, which is enough to see it used. */
private class FlippingCipher : SecretCipher {
    override fun encrypt(plaintext: ByteArray) = ByteArray(plaintext.size) { (plaintext[it].toInt() xor 0x5A).toByte() }

    override fun decrypt(payload: ByteArray) = ByteArray(payload.size) { (payload[it].toInt() xor 0x5A).toByte() }
}

/**
 * Stands in for AES-GCM, whose provider does not start reliably in the unit test JVM after Robolectric ran. It hides
 * the content with the key and adds a checksum, so a changed file is refused like GCM refuses it. The real cipher was
 * seen working on a device.
 */
private class ChecksumContentCipher : ContentCipher {
    override fun encrypt(key: ByteArray, initializationVector: ByteArray, plain: ByteArray): ByteArray {
        val hidden = mask(key, initializationVector, plain)
        return hidden + checksum(hidden)
    }

    override fun decrypt(key: ByteArray, initializationVector: ByteArray, encrypted: ByteArray): ByteArray {
        val hidden = encrypted.copyOfRange(0, encrypted.size - 1)
        require(encrypted.last() == checksum(hidden)[0]) { "The file was changed" }
        return mask(key, initializationVector, hidden)
    }

    private fun mask(key: ByteArray, initializationVector: ByteArray, input: ByteArray) = ByteArray(input.size) {
        val keyByte = key[it % key.size].toInt()
        (input[it].toInt() xor keyByte xor initializationVector[0].toInt()).toByte()
    }

    private fun checksum(bytes: ByteArray) = byteArrayOf(bytes.fold(0) { sum, byte -> sum + byte }.toByte())
}

/** A plain JVM test. */
class AttachmentVaultTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var directory: File
    private lateinit var vault: AttachmentVault

    @Before
    fun createVault() {
        directory = folder.newFolder("stored")
        val context = mockk<Context> { every { cacheDir } returns folder.newFolder("cache") }
        vault = AttachmentVault(FlippingCipher(), ChecksumContentCipher(), context)
    }

    private fun file(name: String, content: ByteArray) = File(directory, name).also { it.writeBytes(content) }

    @Test
    fun `a sealed file does not hold the text and reads back the same`() {
        val content = "A photo of the exam paper".toByteArray()
        val stored = file("a1", content)

        vault.seal(stored)

        assertTrue(vault.isSealed(stored))
        assertFalse(String(stored.readBytes()).contains("exam paper"))
        assertArrayEquals(content, vault.readPlain(stored))
    }

    @Test
    fun `sealing a file twice changes nothing`() {
        val stored = file("a1", "content".toByteArray())
        vault.seal(stored)
        val once = stored.readBytes()

        vault.seal(stored)

        assertArrayEquals(once, stored.readBytes())
    }

    @Test
    fun `every file has its own key and vector`() {
        val first = file("a1", "same".toByteArray())
        val second = file("a2", "same".toByteArray())

        vault.seal(first)
        vault.seal(second)

        assertFalse(first.readBytes().contentEquals(second.readBytes()))
    }

    @Test
    fun `a plain file is read as it is`() {
        val stored = file("a1", "plain".toByteArray())

        assertFalse(vault.isSealed(stored))
        assertEquals("plain", String(vault.readPlain(stored)))
    }

    @Test
    fun `a changed sealed file is refused instead of read`() {
        val stored = file("a1", "content".toByteArray())
        vault.seal(stored)
        val bytes = stored.readBytes()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 1).toByte()
        stored.writeBytes(bytes)

        val failed = runCatching { vault.readPlain(stored) }.isFailure

        assertTrue(failed)
    }

    @Test
    fun `a file waiting to be sent is lent as a readable temporary copy and the copy goes away`() = runTest {
        val content = "photo".toByteArray()
        val stored = file("a1", content)
        vault.seal(stored)
        var lent: File? = null

        val read = vault.withPlainFile(stored) { plain ->
            lent = plain
            plain.readBytes()
        }

        assertArrayEquals(content, read)
        assertFalse(lent!!.exists())
        assertTrue(vault.isSealed(stored))
    }

    @Test
    fun `a plain file is lent as it is`() = runTest {
        val stored = file("a1", "photo".toByteArray())

        val lent = vault.withPlainFile(stored) { it }

        assertEquals(stored, lent)
    }

    @Test
    fun `files that wait to be sent or downloaded are not sealed, the rest are`() {
        val waiting = file("a1.raw", "raw".toByteArray())
        val partial = file("a2.part", "half".toByteArray())
        val settled = file("a3", "done".toByteArray())

        vault.sealSettled(directory)

        assertFalse(vault.isSealed(waiting))
        assertFalse(vault.isSealed(partial))
        assertTrue(vault.isSealed(settled))
    }
}
