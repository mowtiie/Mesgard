package com.mowtiie.mesgard.domain.network

/** How the app reaches its server. The server is unreachable when the device has a network but the connection fails. */
internal enum class ConnectionStatus {
    ONLINE,
    OFFLINE,
    SERVER_UNREACHABLE,
}
