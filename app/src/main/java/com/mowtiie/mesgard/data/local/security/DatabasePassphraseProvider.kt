package com.mowtiie.mesgard.data.local.security

import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.security.SecureRandom

internal class DatabasePassphraseProvider(
    storageFile: File,
    private val cipher: SecretCipher,
    private val random: SecureRandom = SecureRandom(),
) {
    private val storage = AtomicFile(storageFile)

    fun getOrCreate(): ByteArray = if (storage.baseFile.exists()) {
        cipher.decrypt(storage.readFully())
    } else {
        createAndStore()
    }

    private fun createAndStore(): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_BYTES).also(random::nextBytes)
        val encryptedPassphrase = cipher.encrypt(passphrase)
        val stream = storage.startWrite()
        try {
            stream.write(encryptedPassphrase)
            storage.finishWrite(stream)
        } catch (exception: IOException) {
            storage.failWrite(stream)
            throw exception
        }
        return passphrase
    }

    private companion object {
        const val PASSPHRASE_BYTES = 32
    }
}
