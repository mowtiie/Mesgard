package com.mowtiie.mesgard.data.server

import com.mowtiie.mesgard.domain.server.ServerCheckResult
import com.mowtiie.mesgard.domain.server.ServerChecker
import com.mowtiie.mesgard.domain.server.ServerConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

private const val SETTINGS_PATH = "/auth/v1/settings"
private const val TIMEOUT_MILLIS = 8_000
private const val HTTP_OK = 200
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403

/**
 * Asks the Auth service for its public settings with the access key. A Supabase server answers with a JSON object
 * and rejects a key it does not know, so one request tells the address and the key apart.
 */
internal class HttpServerChecker @Inject constructor() : ServerChecker {
    override suspend fun check(config: ServerConfig): ServerCheckResult = withContext(Dispatchers.IO) {
        try {
            request(config)
        } catch (_: IOException) {
            ServerCheckResult.Unreachable
        }
    }

    private fun request(config: ServerConfig): ServerCheckResult {
        val connection = URL(config.url + SETTINGS_PATH).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", config.anonKey)
            return when (connection.responseCode) {
                HTTP_OK -> resultOf(connection.inputStream.bufferedReader().readText())
                HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> ServerCheckResult.KeyRejected
                else -> ServerCheckResult.NotASupabaseServer
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun resultOf(body: String): ServerCheckResult =
        if (runCatching { Json.parseToJsonElement(body).jsonObject }.isSuccess) {
            ServerCheckResult.Reachable
        } else {
            ServerCheckResult.NotASupabaseServer
        }
}
