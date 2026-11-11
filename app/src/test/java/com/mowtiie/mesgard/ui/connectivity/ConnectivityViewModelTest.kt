package com.mowtiie.mesgard.ui.connectivity

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.network.ConnectionStatus
import com.mowtiie.mesgard.domain.network.ConnectivityMonitor
import com.mowtiie.mesgard.domain.sync.RealtimeSync
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val online = MutableStateFlow(true)
    private val serverConnected = MutableStateFlow(true)
    private val monitor = object : ConnectivityMonitor {
        override val isOnline = online
    }
    private val realtimeSync = mockk<RealtimeSync> { every { isConnected } returns serverConnected }

    private fun viewModel() = ConnectivityViewModel(monitor, realtimeSync)

    @Test
    fun `is online while the device has a network and the server is reachable`() = runTest {
        viewModel().status.test { assertEquals(ConnectionStatus.ONLINE, expectMostRecentItem()) }
    }

    @Test
    fun `becomes offline only after the network has been missing for a moment`() = runTest {
        val viewModel = viewModel()

        viewModel.status.test {
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
            online.value = false
            runCurrent()
            advanceTimeBy(OFFLINE_DELAY_MS - 1)
            expectNoEvents()
            advanceTimeBy(2)
            assertEquals(ConnectionStatus.OFFLINE, awaitItem())
        }
    }

    @Test
    fun `a brief network change does not show the offline state`() = runTest {
        val viewModel = viewModel()

        viewModel.status.test {
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
            online.value = false
            runCurrent()
            advanceTimeBy(OFFLINE_DELAY_MS / 2)
            online.value = true
            runCurrent()
            advanceTimeBy(OFFLINE_DELAY_MS * 2)
            expectNoEvents()
        }
    }

    @Test
    fun `is online again as soon as the network returns`() = runTest {
        val viewModel = viewModel()

        viewModel.status.test {
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
            online.value = false
            runCurrent()
            advanceTimeBy(OFFLINE_DELAY_MS + 1)
            assertEquals(ConnectionStatus.OFFLINE, awaitItem())
            online.value = true
            runCurrent()
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
        }
    }

    @Test
    fun `shows the offline state after the delay when there is no network at start`() = runTest {
        online.value = false
        val viewModel = viewModel()

        viewModel.status.test {
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
            advanceTimeBy(OFFLINE_DELAY_MS + 1)
            assertEquals(ConnectionStatus.OFFLINE, awaitItem())
        }
    }

    @Test
    fun `reports an unreachable server after the connection has been down for a moment`() = runTest {
        val viewModel = viewModel()

        viewModel.status.test {
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
            serverConnected.value = false
            runCurrent()
            advanceTimeBy(SERVER_UNREACHABLE_DELAY_MS - 1)
            expectNoEvents()
            advanceTimeBy(2)
            assertEquals(ConnectionStatus.SERVER_UNREACHABLE, awaitItem())
        }
    }

    @Test
    fun `a reconnection that is quick enough shows nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.status.test {
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
            serverConnected.value = false
            runCurrent()
            advanceTimeBy(SERVER_UNREACHABLE_DELAY_MS / 2)
            serverConnected.value = true
            runCurrent()
            advanceTimeBy(SERVER_UNREACHABLE_DELAY_MS * 2)
            expectNoEvents()
        }
    }

    @Test
    fun `is online again as soon as the server is reachable`() = runTest {
        val viewModel = viewModel()

        viewModel.status.test {
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
            serverConnected.value = false
            runCurrent()
            advanceTimeBy(SERVER_UNREACHABLE_DELAY_MS + 1)
            assertEquals(ConnectionStatus.SERVER_UNREACHABLE, awaitItem())
            serverConnected.value = true
            runCurrent()
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
        }
    }

    @Test
    fun `reports no network rather than an unreachable server when both are down`() = runTest {
        val viewModel = viewModel()

        viewModel.status.test {
            assertEquals(ConnectionStatus.ONLINE, awaitItem())
            online.value = false
            serverConnected.value = false
            runCurrent()
            advanceTimeBy(OFFLINE_DELAY_MS + 1)
            assertEquals(ConnectionStatus.OFFLINE, awaitItem())
        }
    }
}
