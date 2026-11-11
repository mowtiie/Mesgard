package com.mowtiie.mesgard.domain.lock

import kotlinx.coroutines.flow.StateFlow

internal interface AppLock {
    val isLocked: StateFlow<Boolean>

    fun onAppBackgrounded()

    fun onAppForegrounded()

    fun unlock()
}
