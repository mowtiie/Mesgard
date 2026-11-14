package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.repository.MessageRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class FailedMessagesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<MessageRepository>()

    @Test
    fun `retries a failed message through the repository`() = runTest {
        coEvery { repository.retry("failed") } returns Unit

        FailedMessagesViewModel(repository).onRetryClicked("failed")

        coVerify(exactly = 1) { repository.retry("failed") }
    }

    @Test
    fun `discards a failed message through the repository`() = runTest {
        coEvery { repository.discard("failed") } returns Unit

        FailedMessagesViewModel(repository).onDiscardClicked("failed")

        coVerify(exactly = 1) { repository.discard("failed") }
    }
}
