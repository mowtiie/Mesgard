package com.mowtiie.mesgard.ui.connectivity

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.network.ConnectivityMonitor
import com.mowtiie.mesgard.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val online = MutableStateFlow(true)
    private val monitor = object : ConnectivityMonitor {
        override val isOnline = online
    }

    @Test
    fun `is not offline while the device has a network`() = runTest {
        ConnectivityViewModel(monitor).isOffline.test {
            assertFalse(expectMostRecentItem())
        }
    }

    @Test
    fun `becomes offline only after the network has been missing for a moment`() = runTest {
        val viewModel = ConnectivityViewModel(monitor)

        viewModel.isOffline.test {
            assertFalse(awaitItem())
            online.value = false
            runCurrent()
            advanceTimeBy(OFFLINE_DELAY_MS - 1)
            expectNoEvents()
            advanceTimeBy(2)
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `a brief network change does not show the offline state`() = runTest {
        val viewModel = ConnectivityViewModel(monitor)

        viewModel.isOffline.test {
            assertFalse(awaitItem())
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
        val viewModel = ConnectivityViewModel(monitor)

        viewModel.isOffline.test {
            assertFalse(awaitItem())
            online.value = false
            runCurrent()
            advanceTimeBy(OFFLINE_DELAY_MS + 1)
            assertTrue(awaitItem())
            online.value = true
            runCurrent()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `shows the offline state after the delay when there is no network at start`() = runTest {
        online.value = false
        val viewModel = ConnectivityViewModel(monitor)

        viewModel.isOffline.test {
            assertFalse(awaitItem())
            advanceTimeBy(OFFLINE_DELAY_MS + 1)
            assertTrue(awaitItem())
        }
    }
}
