package com.mowtiie.mesgard.data.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class KeystoreSecretCipher : SecretCipher {
    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher.iv + cipher.doFinal(plaintext)
    }

    override fun decrypt(payload: ByteArray): ByteArray {
        val initializationVector = payload.copyOfRange(0, INITIALIZATION_VECTOR_BYTES)
        val ciphertext = payload.copyOfRange(INITIALIZATION_VECTOR_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameters = GCMParameterSpec(AUTHENTICATION_TAG_BITS, initializationVector)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), parameters)
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey?
        return existingKey ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val specification = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(specification) }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mesgard_database_key_wrapper"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val INITIALIZATION_VECTOR_BYTES = 12
        const val AUTHENTICATION_TAG_BITS = 128
    }
}
