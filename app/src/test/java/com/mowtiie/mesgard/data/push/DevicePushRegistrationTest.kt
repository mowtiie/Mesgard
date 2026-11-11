package com.mowtiie.mesgard.data.push

import com.mowtiie.mesgard.data.remote.PushSubscriptionRemoteSource
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.push.PushEndpoint
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DevicePushRegistrationTest {
    private val endpoint = PushEndpoint("https://push.example.org/abc", "public-key", "auth-secret")
    private var storedEndpoint: PushEndpoint? = null
    private val store = object : PushEndpointStore {
        override fun read() = storedEndpoint

        override fun save(endpoint: PushEndpoint) {
            storedEndpoint = endpoint
        }

        override fun clear() {
            storedEndpoint = null
        }
    }
    private var chosenDistributor: String? = null
    private val client = mockk<PushClient>(relaxed = true) {
        every { distributorName() } answers { chosenDistributor }
    }
    private val remoteSource = mockk<PushSubscriptionRemoteSource>(relaxed = true)
    private val registration by lazy { DevicePushRegistration(client, store, remoteSource) }

    @Test
    fun `saving an endpoint keeps it and sends it to the server`() = runTest {
        val result = registration.saveEndpoint(endpoint)

        assertEquals(RefreshResult.Success, result)
        assertEquals(endpoint, storedEndpoint)
        coVerify(exactly = 1) { remoteSource.register(endpoint) }
    }

    @Test
    fun `an endpoint is kept when the server cannot be reached`() = runTest {
        coEvery { remoteSource.register(any()) } throws IOException("offline")

        val result = registration.saveEndpoint(endpoint)

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        assertEquals(endpoint, storedEndpoint)
    }

    @Test
    fun `starting registers with the chosen distributor and resends the stored endpoint`() = runTest {
        chosenDistributor = "ntfy"
        storedEndpoint = endpoint

        val result = registration.start()

        assertEquals(RefreshResult.Success, result)
        verify(exactly = 1) { client.register() }
        coVerify(exactly = 1) { remoteSource.register(endpoint) }
        assertEquals("ntfy", registration.distributorName.value)
    }

    @Test
    fun `starting without a distributor or an endpoint does nothing`() = runTest {
        val result = registration.start()

        assertEquals(RefreshResult.Success, result)
        verify(exactly = 0) { client.register() }
        coVerify(exactly = 0) { remoteSource.register(any()) }
    }

    @Test
    fun `stopping removes the subscription, unregisters and forgets the endpoint`() = runTest {
        chosenDistributor = "ntfy"
        storedEndpoint = endpoint

        val result = registration.stop()

        assertEquals(RefreshResult.Success, result)
        coVerify(exactly = 1) { remoteSource.unregister(endpoint.url) }
        verify(exactly = 1) { client.unregister() }
        assertNull(storedEndpoint)
    }

    @Test
    fun `stopping forgets the endpoint even when the server cannot be reached`() = runTest {
        chosenDistributor = "ntfy"
        storedEndpoint = endpoint
        coEvery { remoteSource.unregister(any()) } throws IOException("offline")

        val result = registration.stop()

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), result)
        assertNull(storedEndpoint)
        verify(exactly = 1) { client.unregister() }
    }

    @Test
    fun `forgetting an endpoint removes it from the server and the device`() = runTest {
        storedEndpoint = endpoint

        registration.forgetEndpoint()

        coVerify(exactly = 1) { remoteSource.unregister(endpoint.url) }
        assertNull(storedEndpoint)
    }

    @Test
    fun `choosing a distributor registers with it`() = runTest {
        chosenDistributor = "ntfy"

        registration.onDistributorChosen()

        verify(exactly = 1) { client.register() }
        assertEquals("ntfy", registration.distributorName.value)
    }
}
