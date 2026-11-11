package com.mowtiie.mesgard.domain.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

internal interface RealtimeSync {
    val reconnections: SharedFlow<Unit>

    /** False while the realtime connection to the server is down. True when it is up or has not been started. */
    val isConnected: StateFlow<Boolean>

    fun start(scope: CoroutineScope)

    fun stop()
}
