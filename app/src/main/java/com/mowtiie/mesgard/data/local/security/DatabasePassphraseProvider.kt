package com.mowtiie.mesgard.data.local.security

import java.security.SecureRandom

internal class DatabasePassphraseProvider(
    private val storage: EncryptedFile,
    private val random: SecureRandom = SecureRandom(),
) {
    fun getOrCreate(): ByteArray = storage.read() ?: createAndStore()

    private fun createAndStore(): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_BYTES).also(random::nextBytes)
        storage.write(passphrase)
        return passphrase
    }

    private companion object {
        const val PASSPHRASE_BYTES = 32
    }
}
