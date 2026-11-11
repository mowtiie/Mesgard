package com.mowtiie.mesgard.domain.network

import kotlinx.coroutines.flow.Flow

internal interface ConnectivityMonitor {
    /**
     * Whether the device has a network. A network without internet access still counts, because an institution can
     * host its server on a campus network that is not connected to the internet.
     */
    val isOnline: Flow<Boolean>
}
