package com.mowtiie.mesgard.ui.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.mesgard.domain.network.ConnectionStatus
import com.mowtiie.mesgard.domain.network.ConnectivityMonitor
import com.mowtiie.mesgard.domain.sync.RealtimeSync
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
internal const val OFFLINE_DELAY_MS = 1_000L
internal const val SERVER_UNREACHABLE_DELAY_MS = 3_000L

@HiltViewModel
internal class ConnectivityViewModel @Inject constructor(monitor: ConnectivityMonitor, realtimeSync: RealtimeSync) :
    ViewModel() {
    /**
     * A problem is reported only after it has lasted for a moment, so a brief network change or a reconnection
     * shows nothing. Recovery is reported at once.
     */
    val status: StateFlow<ConnectionStatus> = combine(monitor.isOnline, realtimeSync.isConnected) { online, connected ->
        when {
            !online -> ConnectionStatus.OFFLINE
            !connected -> ConnectionStatus.SERVER_UNREACHABLE
            else -> ConnectionStatus.ONLINE
        }
    }
        .transformLatest { current ->
            when (current) {
                ConnectionStatus.ONLINE -> Unit
                ConnectionStatus.OFFLINE -> delay(OFFLINE_DELAY_MS)
                ConnectionStatus.SERVER_UNREACHABLE -> delay(SERVER_UNREACHABLE_DELAY_MS)
            }
            emit(current)
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ConnectionStatus.ONLINE,
        )
}
