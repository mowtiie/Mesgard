package com.mowtiie.mesgard.data.auth

import com.mowtiie.mesgard.data.local.security.EncryptedFile
import com.mowtiie.mesgard.data.local.security.SecretCipher
import io.github.jan.supabase.auth.user.UserSession
import java.io.File
import java.security.GeneralSecurityException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EncryptedSessionManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val sessionFile: File by lazy { File(temporaryFolder.root, "session.bin") }

    @Test
    fun `loads the session that was saved`() = runTest {
        val manager = sessionManager(ReversingCipher)
        manager.saveSession(session())

        val loaded = manager.loadSessionOrNull()

        assertEquals(ACCESS_TOKEN, loaded?.accessToken)
        assertEquals(REFRESH_TOKEN, loaded?.refreshToken)
    }

    @Test
    fun `stores the session only in encrypted form`() = runTest {
        sessionManager(ReversingCipher).saveSession(session())

        assertFalse(sessionFile.readText().contains(ACCESS_TOKEN))
    }

    @Test
    fun `returns no session when nothing was saved`() = runTest {
        assertNull(sessionManager(ReversingCipher).loadSessionOrNull())
    }

    @Test
    fun `returns no session after the session is deleted`() = runTest {
        val manager = sessionManager(ReversingCipher)
        manager.saveSession(session())

        manager.deleteSession()

        assertNull(manager.loadSessionOrNull())
    }

    @Test
    fun `discards a session that cannot be decrypted`() = runTest {
        sessionManager(ReversingCipher).saveSession(session())

        val loaded = sessionManager(UndecryptableCipher).loadSessionOrNull()

        assertNull(loaded)
        assertFalse(sessionFile.exists())
    }

    @Test
    fun `discards a session that cannot be parsed`() = runTest {
        EncryptedFile(sessionFile, ReversingCipher).write("not a session".encodeToByteArray())

        val loaded = sessionManager(ReversingCipher).loadSessionOrNull()

        assertNull(loaded)
        assertFalse(sessionFile.exists())
    }

    private fun sessionManager(cipher: SecretCipher) = EncryptedSessionManager(EncryptedFile(sessionFile, cipher))

    private fun session() = UserSession(
        accessToken = ACCESS_TOKEN,
        refreshToken = REFRESH_TOKEN,
        expiresIn = EXPIRES_IN_SECONDS,
        tokenType = "bearer",
        user = null,
    )

    private object ReversingCipher : SecretCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = plaintext.reversedArray()

        override fun decrypt(payload: ByteArray): ByteArray = payload.reversedArray()
    }

    private object UndecryptableCipher : SecretCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = plaintext

        override fun decrypt(payload: ByteArray): ByteArray = throw GeneralSecurityException("Key unavailable")
    }

    private companion object {
        const val ACCESS_TOKEN = "access-token-value"
        const val REFRESH_TOKEN = "refresh-token-value"
        const val EXPIRES_IN_SECONDS = 3600L
    }
}
