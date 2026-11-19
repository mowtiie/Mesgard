package com.mowtiie.mesgard.ui.conversation

import androidx.lifecycle.SavedStateHandle
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.MessageReader
import com.mowtiie.mesgard.domain.model.ReadersResult
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.ConversationPreferencesRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.MessageActionsRepository
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationActionsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<MessageActionsRepository>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val preferences = mockk<ConversationPreferencesRepository>(relaxed = true)
    private val viewModel by lazy {
        ConversationActionsViewModel(
            SavedStateHandle(mapOf("conversationId" to "c1")),
            repository,
            conversationRepository,
            preferences,
        )
    }

    @Test
    fun `opening a conversation marked as unread reads it`() = runTest {
        every { conversationRepository.observeConversation("c1") } returns flowOf(summary(isMarkedUnread = true))

        viewModel.onOpened()

        coVerify { preferences.markRead(setOf("c1")) }
    }

    @Test
    fun `opening an ordinary conversation changes nothing`() = runTest {
        every { conversationRepository.observeConversation("c1") } returns flowOf(summary(isMarkedUnread = false))

        viewModel.onOpened()

        coVerify(exactly = 0) { preferences.markRead(any()) }
    }

    private fun summary(isMarkedUnread: Boolean) = ConversationSummary(
        id = "c1",
        type = ConversationType.DIRECT,
        title = "Ana",
        lastMessagePreview = null,
        lastActivityAtEpochMillis = 1L,
        unreadCount = 0,
        canPost = true,
        isMarkedUnread = isMarkedUnread,
    )

    @Test
    fun `info loads who read the message and closes again`() = runTest {
        val readers = listOf(MessageReader("u1", "Ana", 1_000L))
        coEvery { repository.readers("m1") } returns ReadersResult.Loaded(readers)

        viewModel.onInfoRequested("m1")
        assertEquals(ReadersState.Loaded(readers), viewModel.uiState.value.readers)

        viewModel.onInfoDismissed()
        assertNull(viewModel.uiState.value.readers)
    }

    @Test
    fun `info says it failed when the readers cannot be loaded`() = runTest {
        coEvery { repository.readers("m1") } returns ReadersResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        viewModel.onInfoRequested("m1")

        assertEquals(ReadersState.Failed, viewModel.uiState.value.readers)
    }

    @Test
    fun `muting and unmuting go to the server for this conversation`() = runTest {
        coEvery { conversationRepository.setMuted("c1", any()) } returns RefreshResult.Success

        viewModel.onMuteChanged(true)
        viewModel.onMuteChanged(false)

        coVerify { conversationRepository.setMuted("c1", true) }
        coVerify { conversationRepository.setMuted("c1", false) }
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `a mute that fails shows the reason`() = runTest {
        coEvery { conversationRepository.setMuted("c1", true) } returns RefreshResult.Failure(RefreshError.UNKNOWN)

        viewModel.onMuteChanged(true)

        assertEquals(ConversationError.UNKNOWN, viewModel.uiState.value.error)
    }

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
