package com.mowtiie.mesgard.ui.profile

import com.mowtiie.mesgard.domain.repository.ProfileNotice
import com.mowtiie.mesgard.domain.repository.ProfileNoticeKind
import com.mowtiie.mesgard.domain.repository.ProfileNoticeRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileNoticeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ProfileNoticeRepository>()
    private val notice = ProfileNotice("n1", ProfileNoticeKind.USERNAME_RESET)

    @Test
    fun `the notices not seen yet are asked once when the app starts`() = runTest {
        coEvery { repository.unseen() } returns listOf(notice)

        val viewModel = ProfileNoticeViewModel(repository)

        assertEquals(listOf(notice), viewModel.notices.value)
        coVerify(exactly = 1) { repository.unseen() }
    }

    @Test
    fun `acknowledging hides the notice and tells the server`() = runTest {
        coEvery { repository.unseen() } returns listOf(notice)
        coEvery { repository.acknowledge(any()) } returns Unit
        val viewModel = ProfileNoticeViewModel(repository)

        viewModel.onAcknowledged()

        assertTrue(viewModel.notices.value.isEmpty())
        coVerify { repository.acknowledge(listOf(notice)) }
    }

    @Test
    fun `nothing is shown when the server cannot be asked`() = runTest {
        coEvery { repository.unseen() } returns emptyList()

        val viewModel = ProfileNoticeViewModel(repository)

        assertTrue(viewModel.notices.value.isEmpty())
    }
}
