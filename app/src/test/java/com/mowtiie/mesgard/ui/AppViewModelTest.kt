package com.mowtiie.mesgard.ui

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.sync.RealtimeSync
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
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

    @Test
    fun `starts in the loading state until the session is known`() = runTest {
        AppViewModel(authRepository, realtimeSync).authState.test {
            assertEquals(AuthState.Loading, awaitItem())
        }
    }

    @Test
    fun `follows the authentication state of the repository`() = runTest {
        AppViewModel(authRepository, realtimeSync).authState.test {
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

        AppViewModel(authRepository, realtimeSync).onSignOutClicked()

        coVerify(exactly = 1) { authRepository.signOut() }
    }

    @Test
    fun `starts realtime sync when the user is signed in and stops it when signed out`() = runTest {
        AppViewModel(authRepository, realtimeSync)

        authStates.emit(AuthState.SignedIn(USER_ID))
        verify(exactly = 1) { realtimeSync.start(any()) }

        authStates.emit(AuthState.SignedOut)
        verify(atLeast = 1) { realtimeSync.stop() }
    }

    private companion object {
        const val USER_ID = "user-id"
    }
}
