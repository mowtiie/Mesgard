package com.mowtiie.mesgard.data.push

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "PushTokenProvider"

internal interface PushTokenProvider {
    /** The FCM token of this device, or null when Firebase is not configured for this build. */
    suspend fun currentToken(): String?
}

internal class FirebasePushTokenProvider @Inject constructor(@param:ApplicationContext private val context: Context) :
    PushTokenProvider {
    override suspend fun currentToken(): String? {
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.i(TAG, "Firebase is not configured, push notifications are off")
            return null
        }
        return FirebaseMessaging.getInstance().token.await()
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
