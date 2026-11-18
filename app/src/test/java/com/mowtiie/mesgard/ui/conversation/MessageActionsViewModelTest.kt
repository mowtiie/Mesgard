package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MessageActionsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<MessageActionsRepository>()
    private val viewModel by lazy { MessageActionsViewModel(repository) }

    @Test
    fun `deleting asks first and then deletes the message for everyone`() = runTest {
        coEvery { repository.delete("m1") } returns RefreshResult.Success

        viewModel.onDeleteRequested("m1")
        assertEquals("m1", viewModel.uiState.value.messageToDelete)
        viewModel.onDeleteConfirmed()

        assertNull(viewModel.uiState.value.messageToDelete)
        coVerify(exactly = 1) { repository.delete("m1") }
    }

    @Test
    fun `cancelling the question deletes nothing`() = runTest {
        viewModel.onDeleteRequested("m1")
        viewModel.onDeleteDismissed()
        viewModel.onDeleteConfirmed()

        assertNull(viewModel.uiState.value.messageToDelete)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `a failed delete shows the reason until it is shown`() = runTest {
        coEvery { repository.delete("m1") } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        viewModel.onDeleteRequested("m1")
        viewModel.onDeleteConfirmed()
        assertEquals(ConversationError.NETWORK_UNAVAILABLE, viewModel.uiState.value.error)

        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }
}
