package com.mowtiie.mesgard.data.local.security

internal interface SecretCipher {
    fun encrypt(plaintext: ByteArray): ByteArray

    fun decrypt(payload: ByteArray): ByteArray
}
