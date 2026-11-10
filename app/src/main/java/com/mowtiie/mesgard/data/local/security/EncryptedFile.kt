package com.mowtiie.mesgard.data.local.security

import android.util.AtomicFile
import java.io.File
import java.io.IOException

internal class EncryptedFile(file: File, private val cipher: SecretCipher) {
    private val storage = AtomicFile(file)

    fun read(): ByteArray? = if (storage.baseFile.exists()) cipher.decrypt(storage.readFully()) else null

    fun write(plaintext: ByteArray) {
        val encrypted = cipher.encrypt(plaintext)
        val stream = storage.startWrite()
        try {
            stream.write(encrypted)
            storage.finishWrite(stream)
        } catch (exception: IOException) {
            storage.failWrite(stream)
            throw exception
        }
    }

    fun delete() = storage.delete()
}
