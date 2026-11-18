package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.MAX_MESSAGE_LENGTH
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MessageEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<MessageActionsRepository>()
    private val viewModel by lazy { MessageEditViewModel(repository) }

    private fun message(body: String = "Hello") = Message(
        id = "m1",
        conversationId = "c1",
        senderId = "me",
        senderName = "Me",
        body = body,
        createdAtEpochMillis = 1_000L,
        isEdited = false,
        isDeleted = false,
        isOwn = true,
        status = MessageStatus.SENT,
    )

    @Test
    fun `editing starts with the current text and cannot be saved until it changes`() {
        viewModel.onEditRequested(message("Hello"))

        assertTrue(viewModel.uiState.value.isEditing)
        assertEquals("Hello", viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onTextChanged("Hello there")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onTextChanged("Hello ")
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `a blank text or one that is too long cannot be saved`() {
        viewModel.onEditRequested(message())

        viewModel.onTextChanged("   ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onTextChanged("x".repeat(MAX_MESSAGE_LENGTH + 1))
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saving sends the text and leaves edit mode`() = runTest {
        coEvery { repository.edit("m1", "Hello there") } returns RefreshResult.Success
        viewModel.onEditRequested(message())
        viewModel.onTextChanged("Hello there")

        viewModel.onSaveClicked()

        coVerify(exactly = 1) { repository.edit("m1", "Hello there") }
        assertFalse(viewModel.uiState.value.isEditing)
    }

    @Test
    fun `a save that fails stays in edit mode with the reason`() = runTest {
        coEvery { repository.edit("m1", any()) } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)
        viewModel.onEditRequested(message())
        viewModel.onTextChanged("Hello there")

        viewModel.onSaveClicked()

        assertTrue(viewModel.uiState.value.isEditing)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(ConversationError.NETWORK_UNAVAILABLE, viewModel.uiState.value.error)
        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `cancelling saves nothing`() = runTest {
        viewModel.onEditRequested(message())
        viewModel.onTextChanged("Changed")

        viewModel.onEditCancelled()
        viewModel.onSaveClicked()

        assertFalse(viewModel.uiState.value.isEditing)
        coVerify(exactly = 0) { repository.edit(any(), any()) }
    }
}
