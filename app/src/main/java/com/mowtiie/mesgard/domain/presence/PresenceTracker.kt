package com.mowtiie.mesgard.domain.presence

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal interface PresenceTracker {
    val onlineUserIds: StateFlow<Set<String>>

    /** Starts tracking after sign in. The user is only announced as online while the app is in the foreground. */
    fun start(scope: CoroutineScope)

    fun stop()

    fun onAppForegrounded()

    fun onAppBackgrounded()
}
