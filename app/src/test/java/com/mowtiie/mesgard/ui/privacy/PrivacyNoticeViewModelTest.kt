package com.mowtiie.mesgard.ui.privacy

import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.model.AdminResult
import com.mowtiie.mesgard.domain.model.PrivacyNotice
import com.mowtiie.mesgard.domain.repository.PrivacyNoticeRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PrivacyNoticeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<PrivacyNoticeRepository>()

    private fun notice(id: String, url: String? = "https://school.example/privacy", done: Boolean = false) =
        PrivacyNotice(id, "School $id", url, null, isAcknowledged = done)

    @Test
    fun `the first notice that is due is put in front of the person`() = runTest {
        coEvery { repository.notices() } returns
            AdminResult.Success(listOf(notice("o1", done = true), notice("o2"), notice("o3")))

        val state = PrivacyNoticeViewModel(repository).uiState.value

        assertEquals("o2", state.pending?.orgId)
        assertFalse(state.isLoading)
    }

    @Test
    fun `nothing is in front of the person when nothing is due`() = runTest {
        coEvery { repository.notices() } returns AdminResult.Success(listOf(notice("o1", url = null)))

        assertNull(PrivacyNoticeViewModel(repository).uiState.value.pending)
    }

    @Test
    fun `an unreachable server does not hold the person back`() = runTest {
        coEvery { repository.notices() } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)

        val state = PrivacyNoticeViewModel(repository).uiState.value

        assertNull(state.pending)
        assertEquals(AdminError.NETWORK_UNAVAILABLE, state.error)
    }

    @Test
    fun `acknowledging a notice asks the server again, so the next one comes`() = runTest {
        coEvery { repository.notices() } returnsMany listOf(
            AdminResult.Success(listOf(notice("o1"), notice("o2"))),
            AdminResult.Success(listOf(notice("o1", done = true), notice("o2"))),
        )
        coEvery { repository.acknowledge("o1") } returns AdminResult.Success(Unit)
        val viewModel = PrivacyNoticeViewModel(repository)

        viewModel.onAcknowledged("o1")

        coVerify { repository.acknowledge("o1") }
        assertEquals("o2", viewModel.uiState.value.pending?.orgId)
        assertFalse(viewModel.uiState.value.isAcknowledging)
    }

    @Test
    fun `a refused acknowledgment keeps the notice in front and says why`() = runTest {
        coEvery { repository.notices() } returns AdminResult.Success(listOf(notice("o1")))
        coEvery { repository.acknowledge("o1") } returns AdminResult.Failure(AdminError.NETWORK_UNAVAILABLE)
        val viewModel = PrivacyNoticeViewModel(repository)

        viewModel.onAcknowledged("o1")

        assertEquals("o1", viewModel.uiState.value.pending?.orgId)
        assertEquals(AdminError.NETWORK_UNAVAILABLE, viewModel.uiState.value.error)
    }
}
