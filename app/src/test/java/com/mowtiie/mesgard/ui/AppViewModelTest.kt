package com.mowtiie.mesgard.ui

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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

    @Test
    fun `starts in the loading state until the session is known`() = runTest {
        AppViewModel(authRepository).authState.test {
            assertEquals(AuthState.Loading, awaitItem())
        }
    }

    @Test
    fun `follows the authentication state of the repository`() = runTest {
        AppViewModel(authRepository).authState.test {
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

        AppViewModel(authRepository).onSignOutClicked()

        coVerify(exactly = 1) { authRepository.signOut() }
    }

    private companion object {
        const val USER_ID = "user-id"
    }
}
