package com.mowtiie.mesgard.domain.server

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerInputTest {
    @Test
    fun `an address without a scheme is https`() {
        assertEquals(
            ServerAddress.Valid("https://chat.school.example"),
            parseServerAddress("  chat.school.example/ ", allowCleartext = false),
        )
    }

    @Test
    fun `keeps the port and a path but drops a trailing slash, the query and the case of the host`() {
        assertEquals(
            ServerAddress.Valid("https://chat.school.example:8443/api"),
            parseServerAddress("HTTPS://Chat.School.Example:8443/api/?x=1", allowCleartext = false),
        )
    }

    @Test
    fun `refuses plain http unless cleartext is allowed`() {
        assertEquals(ServerAddress.CleartextNotAllowed, parseServerAddress("http://10.0.2.2:54321", false))
        assertEquals(ServerAddress.Valid("http://10.0.2.2:54321"), parseServerAddress("http://10.0.2.2:54321", true))
    }

    @Test
    fun `refuses an empty address, another scheme, a missing host and credentials in the address`() {
        assertEquals(ServerAddress.Empty, parseServerAddress("   ", true))
        assertEquals(ServerAddress.Invalid, parseServerAddress("ftp://chat.school.example", true))
        assertEquals(ServerAddress.Invalid, parseServerAddress("https://", true))
        assertEquals(ServerAddress.Invalid, parseServerAddress("https://user:pass@chat.school.example", true))
        assertEquals(ServerAddress.Invalid, parseServerAddress("not a url", true))
    }

    @Test
    fun `accepts the anon key and the publishable key`() {
        assertNull(findAccessKeyProblem(jwt("anon")))
        assertNull(findAccessKeyProblem("sb_publishable_abc123"))
    }

    @Test
    fun `refuses a key that carries the service role or is a secret key`() {
        assertEquals(AccessKeyProblem.SECRET_KEY, findAccessKeyProblem(jwt("service_role")))
        assertEquals(AccessKeyProblem.SECRET_KEY, findAccessKeyProblem("sb_secret_abc123"))
    }

    @Test
    fun `refuses an empty or malformed key`() {
        assertEquals(AccessKeyProblem.EMPTY, findAccessKeyProblem("  "))
        assertEquals(AccessKeyProblem.MALFORMED, findAccessKeyProblem("hello"))
        assertEquals(AccessKeyProblem.MALFORMED, findAccessKeyProblem("a.b.c"))
        assertEquals(AccessKeyProblem.MALFORMED, findAccessKeyProblem(jwt(role = null)))
    }

    @Test
    fun `shows the host of a server`() {
        assertEquals("school.example", ServerConfig("https://school.example/path", "k").host)
        assertEquals("10.0.2.2", ServerConfig("http://10.0.2.2:54321", "k").host)
        assertEquals("not a url", ServerConfig("not a url", "k").host)
    }

    private fun jwt(role: String?): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256"}""".toByteArray())
        val claims = if (role == null) "{}" else """{"role":"$role"}"""
        return "$header.${encoder.encodeToString(claims.toByteArray())}.signature"
    }
}
