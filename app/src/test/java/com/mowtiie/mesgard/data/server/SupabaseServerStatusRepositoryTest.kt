package com.mowtiie.mesgard.data.server

import android.util.Log
import com.mowtiie.mesgard.data.remote.ServerInfoDto
import com.mowtiie.mesgard.data.remote.ServerInfoRemoteSource
import com.mowtiie.mesgard.domain.model.ServerStatus
import com.mowtiie.mesgard.domain.server.REQUIRED_API_LEVEL
import com.mowtiie.mesgard.domain.server.serverStatusFor
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SupabaseServerStatusRepositoryTest {
    private val remoteSource = mockk<ServerInfoRemoteSource>()
    private val repository = SupabaseServerStatusRepository(remoteSource)

    @Before
    fun silenceLogging() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun restoreLogging() {
        unmockkStatic(Log::class)
    }

    private fun refusal(statusCode: Int) = mockk<RestException> { every { this@mockk.statusCode } returns statusCode }

    @Test
    fun `a server at the needed level is current`() = runTest {
        coEvery { remoteSource.readInfo() } returns ServerInfoDto(REQUIRED_API_LEVEL)

        assertEquals(ServerStatus.CURRENT, repository.check())
    }

    @Test
    fun `a server below the needed level is outdated`() {
        assertEquals(ServerStatus.OUTDATED, serverStatusFor(REQUIRED_API_LEVEL - 1))
        assertEquals(ServerStatus.CURRENT, serverStatusFor(REQUIRED_API_LEVEL + 1))
    }

    @Test
    fun `a server that does not have the function is outdated`() = runTest {
        coEvery { remoteSource.readInfo() } throws refusal(statusCode = 404)

        assertEquals(ServerStatus.OUTDATED, repository.check())
    }

    @Test
    fun `any other refusal is unknown, so nobody is warned for nothing`() = runTest {
        coEvery { remoteSource.readInfo() } throws refusal(statusCode = 500)

        assertEquals(ServerStatus.UNKNOWN, repository.check())
    }

    @Test
    fun `an unreachable server or an unreadable answer is unknown`() = runTest {
        coEvery { remoteSource.readInfo() } throws IOException("offline")
        assertEquals(ServerStatus.UNKNOWN, repository.check())

        coEvery { remoteSource.readInfo() } throws mockk<HttpRequestException>()
        assertEquals(ServerStatus.UNKNOWN, repository.check())

        coEvery { remoteSource.readInfo() } throws SerializationException("bad")
        assertEquals(ServerStatus.UNKNOWN, repository.check())
    }
}
