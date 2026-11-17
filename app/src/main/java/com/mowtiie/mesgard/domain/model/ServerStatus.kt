package com.mowtiie.mesgard.domain.model

/** Whether the server has what this version of the app needs. [UNKNOWN] means it could not be asked. */
internal enum class ServerStatus {
    UNKNOWN,
    CURRENT,
    OUTDATED,
}
