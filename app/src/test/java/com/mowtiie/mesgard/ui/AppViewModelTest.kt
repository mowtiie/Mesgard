package com.mowtiie.mesgard.ui

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.lock.AppLock
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.model.DeviceSettings
import com.mowtiie.mesgard.domain.presence.PresenceTracker
import com.mowtiie.mesgard.domain.push.PushRegistration
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.sync.RealtimeSync
import com.mowtiie.mesgard.testing.FakeDeviceSettings
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authStates = MutableSharedFlow<AuthState>()
    private val authRepository = mockk<AuthRepository> {
        every { authState } returns authStates
    }
    private val realtimeSync = mockk<RealtimeSync>(relaxed = true)
    private val presenceTracker = mockk<PresenceTracker>(relaxed = true)
    private val pushRegistration = mockk<PushRegistration>(relaxed = true)
    private val locked = MutableStateFlow(true)
    private val appLock = mockk<AppLock>(relaxed = true) { every { isLocked } returns locked }
    private val deviceSettings = FakeDeviceSettings(DeviceSettings(appLockEnabled = true))

    private fun viewModel() =
        AppViewModel(authRepository, realtimeSync, presenceTracker, pushRegistration, appLock, deviceSettings)

    @Test
    fun `starts in the loading state until the session is known`() = runTest {
        viewModel().authState.test {
            assertEquals(AuthState.Loading, awaitItem())
        }
    }

    @Test
    fun `follows the authentication state of the repository`() = runTest {
        viewModel().authState.test {
            assertEquals(AuthState.Loading, awaitItem())

            authStates.emit(AuthState.SignedIn(USER_ID))
            assertEquals(AuthState.SignedIn(USER_ID), awaitItem())

            authStates.emit(AuthState.SignedOut)
            assertEquals(AuthState.SignedOut, awaitItem())
        }
    }

    @Test
    fun `signs out through the repository`() {
        coEvery { authRepository.signOut() } returns Unit

        viewModel().onSignOutClicked()

        coVerifyOrder {
            pushRegistration.unregister()
            authRepository.signOut()
        }
    }

    @Test
    fun `starts realtime sync when the user is signed in and stops it when signed out`() = runTest {
        viewModel()

        authStates.emit(AuthState.SignedIn(USER_ID))
        verify(exactly = 1) { realtimeSync.start(any()) }
        verify(exactly = 1) { presenceTracker.start(any()) }
        coVerify(exactly = 1) { pushRegistration.register() }

        authStates.emit(AuthState.SignedOut)
        verify(atLeast = 1) { realtimeSync.stop() }
        verify(atLeast = 1) { presenceTracker.stop() }
    }

    @Test
    fun `reports locked while app lock is enabled and locked`() = runTest {
        viewModel().isLocked.test { assertEquals(true, expectMostRecentItem()) }
    }

    @Test
    fun `reports unlocked once the lock is cleared`() = runTest {
        viewModel().isLocked.test {
            assertEquals(true, expectMostRecentItem())
            locked.value = false
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `does not report locked when app lock is turned off`() = runTest {
        deviceSettings.setAppLockEnabled(false)

        viewModel().isLocked.test { assertEquals(false, expectMostRecentItem()) }
    }

    @Test
    fun `keeps the lock while the session is still loading`() = runTest {
        viewModel()

        authStates.emit(AuthState.Loading)

        verify(exactly = 0) { appLock.unlock() }
    }

    @Test
    fun `clears the lock when the user is signed out`() = runTest {
        viewModel()

        authStates.emit(AuthState.SignedOut)

        verify(atLeast = 1) { appLock.unlock() }
    }

    private companion object {
        const val USER_ID = "user-id"
    }
}
