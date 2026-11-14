package com.mowtiie.mesgard.domain.server

import java.net.URI
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val SECRET_KEY_PREFIX = "sb_secret_"
private const val PUBLISHABLE_KEY_PREFIX = "sb_publishable_"
private const val ANON_ROLE = "anon"
private const val JWT_PARTS = 3
private const val PAYLOAD_INDEX = 1

internal sealed interface ServerAddress {
    data class Valid(val url: String) : ServerAddress

    data object Empty : ServerAddress

    data object Invalid : ServerAddress

    /** A plain http address, which release builds do not allow. */
    data object CleartextNotAllowed : ServerAddress
}

internal enum class AccessKeyProblem {
    EMPTY,
    MALFORMED,
    SECRET_KEY,
}

/**
 * Turns what a person typed into the base address of the server: scheme, host, optional port and optional path
 * (a reverse proxy may serve Supabase under a path). A missing scheme means https.
 */
internal fun parseServerAddress(input: String, allowCleartext: Boolean): ServerAddress {
    val text = input.trim()
    if (text.isEmpty()) return ServerAddress.Empty
    val withScheme = if ("://" in text) text else "https://$text"
    val uri = runCatching { URI(withScheme) }.getOrNull()
    val scheme = uri?.scheme?.lowercase()
    val host = uri?.host
    return when {
        uri == null || host.isNullOrBlank() || uri.userInfo != null -> ServerAddress.Invalid
        scheme == "https" || (scheme == "http" && allowCleartext) -> ServerAddress.Valid(baseOf(uri, scheme, host))
        scheme == "http" -> ServerAddress.CleartextNotAllowed
        else -> ServerAddress.Invalid
    }
}

private fun baseOf(uri: URI, scheme: String, host: String): String {
    val port = if (uri.port >= 0) ":${uri.port}" else ""
    return "$scheme://${host.lowercase()}$port${uri.path.orEmpty().trimEnd('/')}"
}

/**
 * Only the public anon key belongs in the app. A key that carries the service role (or the newer secret key) would
 * give every user of the app full access to the database, so it is refused.
 */
internal fun findAccessKeyProblem(input: String): AccessKeyProblem? {
    val key = input.trim()
    return when {
        key.isEmpty() -> AccessKeyProblem.EMPTY
        key.startsWith(SECRET_KEY_PREFIX) -> AccessKeyProblem.SECRET_KEY
        key.startsWith(PUBLISHABLE_KEY_PREFIX) -> null
        else -> when (roleOf(key)) {
            ANON_ROLE -> null
            null -> AccessKeyProblem.MALFORMED
            else -> AccessKeyProblem.SECRET_KEY
        }
    }
}

private fun roleOf(jwt: String): String? {
    val parts = jwt.split('.')
    if (parts.size != JWT_PARTS) return null
    return runCatching {
        val payload = String(Base64.getUrlDecoder().decode(parts[PAYLOAD_INDEX]))
        Json.parseToJsonElement(payload).jsonObject["role"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}
