package com.mowtiie.mesgard.data.auth

import android.util.Log
import com.mowtiie.mesgard.data.local.security.EncryptedFile
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import java.security.GeneralSecurityException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "EncryptedSessionManager"

internal class EncryptedSessionManager(private val storage: EncryptedFile, private val json: Json = Json) :
    SessionManager {
    override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        storage.write(json.encodeToString(session).encodeToByteArray())
    }

    override suspend fun loadSession(): UserSession = loadSessionOrNull() ?: throw NoSessionFoundException()

    override suspend fun loadSessionOrNull(): UserSession? = withContext(Dispatchers.IO) {
        try {
            storage.read()?.let { json.decodeFromString<UserSession>(it.decodeToString()) }
        } catch (exception: GeneralSecurityException) {
            discardUnreadableSession(exception)
        } catch (exception: SerializationException) {
            discardUnreadableSession(exception)
        }
    }

    override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        storage.delete()
    }

    private fun discardUnreadableSession(cause: Exception): UserSession? {
        Log.w(TAG, "Discarding a stored session that cannot be read; the user must sign in again", cause)
        storage.delete()
        return null
    }
}
