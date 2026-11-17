package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.ServerStatus

internal interface ServerStatusRepository {
    /** Asks the server for its API level, also before sign in. Answers [ServerStatus.UNKNOWN] when it cannot. */
    suspend fun check(): ServerStatus
}
