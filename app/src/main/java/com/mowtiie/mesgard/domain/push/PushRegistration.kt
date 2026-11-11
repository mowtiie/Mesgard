package com.mowtiie.mesgard.domain.push

import com.mowtiie.mesgard.domain.model.RefreshResult

internal interface PushRegistration {
    /** Registers the FCM token of this device for the signed in user. Does nothing when push is not configured. */
    suspend fun register(): RefreshResult

    /** Removes the FCM token of this device from the signed in user. Does nothing when push is not configured. */
    suspend fun unregister(): RefreshResult
}
