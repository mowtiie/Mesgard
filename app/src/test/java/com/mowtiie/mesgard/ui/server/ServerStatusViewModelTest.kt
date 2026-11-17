package com.mowtiie.mesgard.ui.server

import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.model.ServerStatus
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.repository.ServerStatusRepository
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

class ServerStatusViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authStates = MutableSharedFlow<AuthState>()
    private val authRepository = mockk<AuthRepository> { every { authState } returns authStates }
    private val repository = mockk<ServerStatusRepository>()

    @Test
    fun `checks the server when it opens and reports the answer`() {
        coEvery { repository.check() } returns ServerStatus.OUTDATED

        assertEquals(ServerStatus.OUTDATED, ServerStatusViewModel(repository, authRepository).status.value)
    }

    @Test
    fun `asks again after sign in when the first answer was unknown`() = runTest {
        coEvery { repository.check() } returnsMany listOf(ServerStatus.UNKNOWN, ServerStatus.CURRENT)
        val viewModel = ServerStatusViewModel(repository, authRepository)

        authStates.emit(AuthState.SignedIn("user"))

        coVerify(exactly = 2) { repository.check() }
        assertEquals(ServerStatus.CURRENT, viewModel.status.value)
    }

    @Test
    fun `does not ask again when the server already answered`() = runTest {
        coEvery { repository.check() } returns ServerStatus.CURRENT
        ServerStatusViewModel(repository, authRepository)

        authStates.emit(AuthState.SignedIn("user"))
        authStates.emit(AuthState.SignedOut)

        coVerify(exactly = 1) { repository.check() }
    }
}
