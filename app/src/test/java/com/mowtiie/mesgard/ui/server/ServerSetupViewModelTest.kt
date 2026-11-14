package com.mowtiie.mesgard.ui.server

import android.net.Uri
import com.mowtiie.mesgard.domain.repository.AuthRepository
import com.mowtiie.mesgard.domain.server.AppRestarter
import com.mowtiie.mesgard.domain.server.ServerCheckResult
import com.mowtiie.mesgard.domain.server.ServerChecker
import com.mowtiie.mesgard.domain.server.ServerConfig
import com.mowtiie.mesgard.domain.server.ServerConfigStore
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ServerSetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var stored: ServerConfig? = null
    private var signedInUserId: String? = null
    private val store = mockk<ServerConfigStore>(relaxed = true) {
        every { current } answers { stored }
        every { save(any()) } answers { stored = firstArg() }
    }
    private val checker = mockk<ServerChecker>()
    private val restarter = mockk<AppRestarter>(relaxed = true)
    private val authRepository = mockk<AuthRepository> { every { currentUserId } answers { signedInUserId } }

    private fun viewModel() = ServerSetupViewModel(store, checker, restarter, authRepository)

    private val anonKey = run {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        "${encoder.encodeToString("{}".toByteArray())}.${encoder.encodeToString("""{"role":"anon"}""".toByteArray())}.s"
    }

    private fun ServerSetupViewModel.fill(address: String = "chat.school.example", key: String = anonKey) {
        onAddressChanged(address)
        onAccessKeyChanged(key)
    }

    @Test
    fun `starts with the current server filled in and can be cancelled only when one is set`() {
        assertFalse(viewModel().uiState.value.canCancel)

        stored = ServerConfig("https://old.example", "old-key")
        val state = viewModel().uiState.value

        assertEquals("https://old.example", state.address)
        assertEquals("old-key", state.accessKey)
        assertTrue(state.canCancel)
    }

    @Test
    fun `saves a server that answers and restarts the app`() = runTest {
        coEvery { checker.check(any()) } returns ServerCheckResult.Reachable
        val viewModel = viewModel().apply { fill() }

        viewModel.onConnectClicked()

        assertEquals(ServerConfig("https://chat.school.example", anonKey), stored)
        verify(exactly = 1) { restarter.restart() }
    }

    @Test
    fun `finishes without a restart when the server did not change`() = runTest {
        stored = ServerConfig("https://chat.school.example", anonKey)
        coEvery { checker.check(any()) } returns ServerCheckResult.Reachable
        val viewModel = viewModel().apply { fill() }

        viewModel.onConnectClicked()

        assertTrue(viewModel.uiState.value.isFinished)
        verify(exactly = 0) { restarter.restart() }
    }

    @Test
    fun `reports what is wrong with the address and the key before contacting the server`() {
        val viewModel = viewModel()

        viewModel.fill(address = "", key = anonKey)
        viewModel.onConnectClicked()
        assertEquals(ServerSetupError.ADDRESS_EMPTY, viewModel.uiState.value.error)

        viewModel.fill(address = "ftp://x", key = anonKey)
        viewModel.onConnectClicked()
        assertEquals(ServerSetupError.ADDRESS_INVALID, viewModel.uiState.value.error)

        viewModel.fill(key = "")
        viewModel.onConnectClicked()
        assertEquals(ServerSetupError.KEY_EMPTY, viewModel.uiState.value.error)

        viewModel.fill(key = "sb_secret_abc")
        viewModel.onConnectClicked()
        assertEquals(ServerSetupError.KEY_SECRET, viewModel.uiState.value.error)
        assertNull(stored)
    }

    @Test
    fun `reports an unreachable server, a rejected key and an address that is not a server`() = runTest {
        val viewModel = viewModel().apply { fill() }

        coEvery { checker.check(any()) } returns ServerCheckResult.Unreachable
        viewModel.onConnectClicked()
        assertEquals(ServerSetupError.UNREACHABLE, viewModel.uiState.value.error)

        coEvery { checker.check(any()) } returns ServerCheckResult.KeyRejected
        viewModel.onConnectClicked()
        assertEquals(ServerSetupError.KEY_REJECTED, viewModel.uiState.value.error)

        coEvery { checker.check(any()) } returns ServerCheckResult.NotASupabaseServer
        viewModel.onConnectClicked()
        assertEquals(ServerSetupError.NOT_A_SERVER, viewModel.uiState.value.error)
        assertNull(stored)
        verify(exactly = 0) { restarter.restart() }
    }

    @Test
    fun `a different server is refused while someone is signed in`() = runTest {
        stored = ServerConfig("https://old.example", "old-key")
        signedInUserId = "user"
        val viewModel = viewModel().apply { fill() }

        viewModel.onConnectClicked()

        assertEquals(ServerSetupError.SIGN_OUT_FIRST, viewModel.uiState.value.error)
        assertEquals(ServerConfig("https://old.example", "old-key"), stored)
    }

    @Test
    fun `a setup link fills the form and shows the warning but does not connect`() {
        val viewModel = viewModel()

        viewModel.onPrefill(ServerSetupPrefill("chat.school.example", "the-key"))

        val state = viewModel.uiState.value
        assertEquals("chat.school.example", state.address)
        assertEquals("the-key", state.accessKey)
        assertTrue(state.isFromLink)
        assertNull(stored)
    }

    @Test
    fun `reads a setup link and ignores other links`() {
        val link = Uri.parse("mesgard://setup?url=https%3A%2F%2Fchat.school.example&key=abc")

        assertEquals(ServerSetupPrefill("https://chat.school.example", "abc"), parseSetupLink(link))
        assertNull(parseSetupLink(Uri.parse("mesgard://setup?url=https%3A%2F%2Fchat.school.example")))
        assertNull(parseSetupLink(Uri.parse("https://setup?url=a&key=b")))
        assertNull(parseSetupLink(null))
    }
}
