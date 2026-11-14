package com.mowtiie.mesgard.data.server

import com.mowtiie.mesgard.domain.server.ServerCheckResult
import com.mowtiie.mesgard.domain.server.ServerConfig
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HttpServerCheckerTest {
    private lateinit var server: HttpServer
    private var status = 200
    private var body = """{"external":{}}"""
    private var receivedKey: String? = null

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/auth/v1/settings") { exchange ->
            receivedKey = exchange.requestHeaders.getFirst("apikey")
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    @After
    fun stopServer() = server.stop(0)

    private fun config() = ServerConfig("http://127.0.0.1:${server.address.port}", "the-key")

    @Test
    fun `a Supabase server that accepts the key is reachable and receives the key`() = runTest {
        assertEquals(ServerCheckResult.Reachable, HttpServerChecker().check(config()))
        assertEquals("the-key", receivedKey)
    }

    @Test
    fun `a server that refuses the key reports a rejected key`() = runTest {
        status = 401

        assertEquals(ServerCheckResult.KeyRejected, HttpServerChecker().check(config()))
    }

    @Test
    fun `an address that answers something else is not a Supabase server`() = runTest {
        body = "<html>Welcome</html>"
        assertEquals(ServerCheckResult.NotASupabaseServer, HttpServerChecker().check(config()))

        status = 404
        assertEquals(ServerCheckResult.NotASupabaseServer, HttpServerChecker().check(config()))
    }

    @Test
    fun `an address nobody listens on is unreachable`() = runTest {
        val unused = config()
        server.stop(0)

        assertEquals(ServerCheckResult.Unreachable, HttpServerChecker().check(unused))
    }
}
