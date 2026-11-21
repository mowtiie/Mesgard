package com.mowtiie.mesgard.data.attachment

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val AUTHENTICATION_TAG_BITS = 128

/** Encrypts and decrypts the content of one file with a key of its own. A changed ciphertext is refused. */
internal interface ContentCipher {
    fun encrypt(key: ByteArray, initializationVector: ByteArray, plain: ByteArray): ByteArray

    fun decrypt(key: ByteArray, initializationVector: ByteArray, encrypted: ByteArray): ByteArray
}

/** AES-256 in Galois/Counter Mode, which also detects a file that was changed. */
internal class AesGcmContentCipher : ContentCipher {
    override fun encrypt(key: ByteArray, initializationVector: ByteArray, plain: ByteArray): ByteArray =
        run(Cipher.ENCRYPT_MODE, key, initializationVector, plain)

    override fun decrypt(key: ByteArray, initializationVector: ByteArray, encrypted: ByteArray): ByteArray =
        run(Cipher.DECRYPT_MODE, key, initializationVector, encrypted)

    private fun run(mode: Int, key: ByteArray, initializationVector: ByteArray, input: ByteArray): ByteArray =
        Cipher.getInstance(TRANSFORMATION).run {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(AUTHENTICATION_TAG_BITS, initializationVector))
            doFinal(input)
        }
}
