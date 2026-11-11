package com.mowtiie.mesgard.data.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
class AndroidConnectivityMonitorTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val manager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val monitor = AndroidConnectivityMonitor(application)

    private fun capabilities(internet: Boolean): NetworkCapabilities {
        val capabilities = ShadowNetworkCapabilities.newInstance()
        if (internet) shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        return capabilities
    }

    @Test
    fun `reports the current network state when collection starts`() = runTest {
        val network = manager.activeNetwork!!
        shadowOf(manager).setNetworkCapabilities(network, capabilities(internet = true))

        monitor.isOnline.test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `follows the default network as it changes`() = runTest {
        val network = manager.activeNetwork!!
        shadowOf(manager).setNetworkCapabilities(network, capabilities(internet = true))

        monitor.isOnline.test {
            assertEquals(true, awaitItem())
            val callback = shadowOf(manager).networkCallbacks.single()

            callback.onLost(network)
            assertEquals(false, awaitItem())

            callback.onCapabilitiesChanged(network, capabilities(internet = true))
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a network without the internet capability counts as offline`() = runTest {
        val network = manager.activeNetwork!!
        shadowOf(manager).setNetworkCapabilities(network, capabilities(internet = false))

        monitor.isOnline.test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stops listening when collection ends`() = runTest {
        monitor.isOnline.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0, shadowOf(manager).networkCallbacks.size)
    }
}
