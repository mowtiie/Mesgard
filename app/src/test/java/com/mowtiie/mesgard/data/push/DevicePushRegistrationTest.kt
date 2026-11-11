package com.mowtiie.mesgard.data.push

import com.google.firebase.FirebaseException
import com.mowtiie.mesgard.data.remote.DeviceTokenRemoteSource
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DevicePushRegistrationTest {
    private val tokenProvider = mockk<PushTokenProvider>()
    private val remoteSource = mockk<DeviceTokenRemoteSource>(relaxed = true)
    private val registration = DevicePushRegistration(tokenProvider, remoteSource)

    @Test
    fun `registers the token of this device`() = runTest {
        coEvery { tokenProvider.currentToken() } returns "token-1"

        val result = registration.register()

        assertEquals(RefreshResult.Success, result)
        coVerify(exactly = 1) { remoteSource.register("token-1") }
    }

    @Test
    fun `unregisters the token of this device`() = runTest {
        coEvery { tokenProvider.currentToken() } returns "token-1"

        val result = registration.unregister()

        assertEquals(RefreshResult.Success, result)
        coVerify(exactly = 1) { remoteSource.unregister("token-1") }
    }

    @Test
    fun `does nothing when push is not configured`() = runTest {
        coEvery { tokenProvider.currentToken() } returns null

        assertEquals(RefreshResult.Success, registration.register())
        assertEquals(RefreshResult.Success, registration.unregister())
        coVerify(exactly = 0) { remoteSource.register(any()) }
        coVerify(exactly = 0) { remoteSource.unregister(any()) }
    }

    @Test
    fun `reports a network failure while reading the token`() = runTest {
        coEvery { tokenProvider.currentToken() } throws IOException("offline")

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), registration.register())
    }

    @Test
    fun `reports a Firebase failure while reading the token`() = runTest {
        coEvery { tokenProvider.currentToken() } throws FirebaseException("unavailable")

        assertEquals(RefreshResult.Failure(RefreshError.UNKNOWN), registration.register())
    }

    @Test
    fun `reports a failure to register with the server`() = runTest {
        coEvery { tokenProvider.currentToken() } returns "token-1"
        coEvery { remoteSource.register(any()) } throws IOException("offline")

        assertEquals(RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE), registration.register())
    }
}
