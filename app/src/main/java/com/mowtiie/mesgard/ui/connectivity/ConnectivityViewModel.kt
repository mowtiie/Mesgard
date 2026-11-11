package com.mowtiie.mesgard.ui.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.network.ConnectivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
internal const val OFFLINE_DELAY_MS = 1_000L

@HiltViewModel
internal class ConnectivityViewModel @Inject constructor(monitor: ConnectivityMonitor) : ViewModel() {
    /** True once the device has been without a network for a moment, so a brief network change shows nothing. */
    val isOffline: StateFlow<Boolean> = monitor.isOnline
        .transformLatest { online ->
            if (online) {
                emit(false)
            } else {
                delay(OFFLINE_DELAY_MS)
                emit(true)
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = false,
        )
}
