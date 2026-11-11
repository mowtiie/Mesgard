package com.mowtiie.mesgard.domain.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

internal interface RealtimeSync {
    val reconnections: SharedFlow<Unit>

    fun start(scope: CoroutineScope)

    fun stop()
}
