package com.mowtiie.mesgard.data.attachment

import android.content.Context
import com.mowtiie.mesgard.data.local.security.SecretCipher
import com.mowtiie.mesgard.domain.attachment.UNOPTIMIZED_SUFFIX
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

internal const val PLAIN_ATTACHMENT_DIRECTORY = "plain_attachments"

private val MAGIC = "MGAF1".toByteArray()
private const val SEALING_SUFFIX = ".sealing"
private const val PARTIAL_SUFFIX = ".part"
private const val DATA_KEY_BYTES = 32
private const val INITIALIZATION_VECTOR_BYTES = 12
private const val LENGTH_BYTES = 2
private const val BYTE_MASK = 0xFF
private const val BITS_PER_BYTE = 8

/**
 * Keeps attachment files encrypted on the phone. Each file gets its own random AES-256 key, and that key is wrapped by
 * the Android Keystore key that already protects the database key, so a copy of the file alone is unreadable. A file is
 * sealed once it is received or sent. A file that waits to be sent stays readable until then, because the photo is
 * shrunk and uploaded from it. Sealing a file twice does nothing.
 */
@Singleton
internal class AttachmentVault @Inject constructor(
    private val cipher: SecretCipher,
    private val content: ContentCipher,
    @ApplicationContext private val context: Context,
) {
    private val random = SecureRandom()

    fun isSealed(file: File): Boolean {
        if (!file.isFile) return false
        val header = ByteArray(MAGIC.size)
        val read = file.inputStream().use { it.read(header) }
        return read == MAGIC.size && header.contentEquals(MAGIC)
    }

    /** Replaces the file with an encrypted one, written beside it first so a failure loses nothing. */
    fun seal(file: File) {
        if (!file.isFile || isSealed(file)) return
        val sealed = encrypt(file.readBytes())
        val temporary = File(file.parentFile, file.name + SEALING_SUFFIX)
        try {
            temporary.writeBytes(sealed)
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            temporary.delete()
        }
    }

    /** The content of the file, decrypted when it is sealed. */
    fun readPlain(file: File): ByteArray {
        val bytes = file.readBytes()
        return if (startsWithMagic(bytes)) decrypt(bytes) else bytes
    }

    fun copyPlain(file: File, target: File) = target.writeBytes(readPlain(file))

    /** Runs [block] with a readable file: the file when it is plain, else a temporary copy removed afterwards. */
    suspend fun <T> withPlainFile(file: File, block: suspend (File) -> T): T {
        if (!isSealed(file)) return block(file)
        val directory = File(context.cacheDir, PLAIN_ATTACHMENT_DIRECTORY).also { it.mkdirs() }
        val temporary = File(directory, file.name)
        try {
            copyPlain(file, temporary)
            return block(temporary)
        } finally {
            temporary.delete()
        }
    }

    /** Seals every file that is not waiting to be sent or being downloaded. A file that cannot be sealed stays. */
    fun sealSettled(directory: File) {
        directory.listFiles().orEmpty()
            .filter { it.isFile && !it.name.endsWith(UNOPTIMIZED_SUFFIX) && !it.name.endsWith(PARTIAL_SUFFIX) }
            .filterNot { it.name.endsWith(SEALING_SUFFIX) }
            .forEach { runCatching { seal(it) } }
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val dataKey = ByteArray(DATA_KEY_BYTES).also(random::nextBytes)
        val wrapped = cipher.encrypt(dataKey)
        val initializationVector = ByteArray(INITIALIZATION_VECTOR_BYTES).also(random::nextBytes)
        val encrypted = content.encrypt(dataKey, initializationVector, plain)
        return ByteArrayOutputStream().apply {
            write(MAGIC)
            write(wrapped.size shr BITS_PER_BYTE)
            write(wrapped.size and BYTE_MASK)
            write(wrapped)
            write(initializationVector)
            write(encrypted)
        }.toByteArray()
    }

    private fun decrypt(sealed: ByteArray): ByteArray {
        var offset = MAGIC.size
        val wrappedLength = (sealed[offset].toInt() and BYTE_MASK shl BITS_PER_BYTE) or
            (sealed[offset + 1].toInt() and BYTE_MASK)
        offset += LENGTH_BYTES
        val dataKey = cipher.decrypt(sealed.copyOfRange(offset, offset + wrappedLength))
        offset += wrappedLength
        val initializationVector = sealed.copyOfRange(offset, offset + INITIALIZATION_VECTOR_BYTES)
        offset += INITIALIZATION_VECTOR_BYTES
        return content.decrypt(dataKey, initializationVector, sealed.copyOfRange(offset, sealed.size))
    }

    private fun startsWithMagic(bytes: ByteArray) =
        bytes.size >= MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
}
