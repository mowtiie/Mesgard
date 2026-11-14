package com.mowtiie.mesgard.domain.server

import java.net.URI

/** The Supabase server the app talks to. The access key is the public anon key, never the service role key. */
internal data class ServerConfig(val url: String, val anonKey: String) {
    /** The host of the server, so people can see which server they are connected to. */
    val host: String
        get() = runCatching { URI(url).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: url
}

internal interface ServerConfigStore {
    /** The server chosen on this device, or the one built into the app, or null when none is set. */
    val current: ServerConfig?

    /** Writes the configuration before returning, because the app restarts right after. */
    fun save(config: ServerConfig)
}

internal sealed interface ServerCheckResult {
    data object Reachable : ServerCheckResult

    data object Unreachable : ServerCheckResult

    data object KeyRejected : ServerCheckResult

    data object NotASupabaseServer : ServerCheckResult
}

internal interface ServerChecker {
    suspend fun check(config: ServerConfig): ServerCheckResult
}

/** Starts the app again so every component is built with the new server. */
internal interface AppRestarter {
    fun restart()
}
