package com.mowtiie.mesgard.domain.lifecycle

import kotlinx.coroutines.flow.StateFlow

internal interface AppVisibility {
    val isForeground: StateFlow<Boolean>

    fun setForeground(foreground: Boolean)
}
