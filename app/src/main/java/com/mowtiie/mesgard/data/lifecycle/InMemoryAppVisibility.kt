package com.mowtiie.mesgard.data.lifecycle

import com.mowtiie.mesgard.domain.lifecycle.AppVisibility
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
internal class InMemoryAppVisibility @Inject constructor() : AppVisibility {
    private val mutableIsForeground = MutableStateFlow(false)

    override val isForeground: StateFlow<Boolean> = mutableIsForeground.asStateFlow()

    override fun setForeground(foreground: Boolean) {
        mutableIsForeground.value = foreground
    }
}
