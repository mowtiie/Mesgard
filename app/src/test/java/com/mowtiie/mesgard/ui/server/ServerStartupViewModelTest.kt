package com.mowtiie.mesgard.ui.server

import com.mowtiie.mesgard.domain.model.AuthState
import com.mowtiie.mesgard.domain.network.ConnectivityMonitor
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.server.ServerCheckResult
import com.mowtiie.mesgard.domain.server.ServerChecker
import com.mowtiie.mesgard.domain.server.ServerConfig
import com.mowtiie.mesgard.domain.server.ServerConfigStore
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ServerStartupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var saved: ServerConfig? = ServerConfig("https://chat.example.org", "key")
    private var authState: AuthState = AuthState.SignedOut
    private var online = true
    private val store = mockk<ServerConfigStore> { every { current } answers { saved } }
    private val checker = mockk<ServerChecker>()
    private val authRepository = mockk<AuthRepository> {
        every { authState } answers { flowOf(AuthState.Loading, this@ServerStartupViewModelTest.authState) }
    }
    private val connectivityMonitor = mockk<ConnectivityMonitor> { every { isOnline } answers { flowOf(online) } }

    private fun startupProblem(result: ServerCheckResult): ServerSetupError? {
        coEvery { checker.check(any()) } returns result
        return ServerStartupViewModel(store, checker, authRepository, connectivityMonitor).problem.value
    }

    @Test
    fun `a server that answers raises no problem`() {
        assertNull(startupProblem(ServerCheckResult.Reachable))
    }

    @Test
    fun `a rejected key and an address that is not a server name what is wrong`() {
        assertEquals(ServerSetupError.KEY_REJECTED, startupProblem(ServerCheckResult.KeyRejected))
        assertEquals(ServerSetupError.NOT_A_SERVER, startupProblem(ServerCheckResult.NotASupabaseServer))
    }

    @Test
    fun `an unreachable server is a problem only when the device has a network`() {
        assertEquals(ServerSetupError.UNREACHABLE, startupProblem(ServerCheckResult.Unreachable))

        online = false
        assertNull(startupProblem(ServerCheckResult.Unreachable))
    }

    @Test
    fun `someone who is signed in is never sent to the setup screen`() = runTest {
        authState = AuthState.SignedIn("user")

        assertNull(startupProblem(ServerCheckResult.KeyRejected))
        coVerify(exactly = 0) { checker.check(any()) }
    }

    @Test
    fun `nothing is checked when no server is saved`() = runTest {
        saved = null

        assertNull(startupProblem(ServerCheckResult.KeyRejected))
        coVerify(exactly = 0) { checker.check(any()) }
    }

    @Test
    fun `the problem is cleared once the setup screen has handled it`() {
        coEvery { checker.check(any()) } returns ServerCheckResult.KeyRejected
        val viewModel = ServerStartupViewModel(store, checker, authRepository, connectivityMonitor)

        viewModel.onProblemHandled()

        assertNull(viewModel.problem.value)
    }
}
