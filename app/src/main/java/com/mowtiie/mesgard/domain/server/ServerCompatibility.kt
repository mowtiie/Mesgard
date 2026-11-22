package com.mowtiie.mesgard.domain.server

import com.mowtiie.mesgard.domain.model.ServerStatus

/**
 * The server API level this version of the app needs. Raise it together with `server_info()` in a new migration when
 * the app starts to use something an older server does not have.
 */
internal const val REQUIRED_API_LEVEL = 16

internal fun serverStatusFor(apiLevel: Int): ServerStatus =
    if (apiLevel >= REQUIRED_API_LEVEL) ServerStatus.CURRENT else ServerStatus.OUTDATED
