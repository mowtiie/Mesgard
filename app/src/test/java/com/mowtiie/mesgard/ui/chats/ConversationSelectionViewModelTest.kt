package com.mowtiie.mesgard.ui.chats

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.PreferenceError
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.repository.ConversationPreferencesRepository
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConversationSelectionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversations = MutableStateFlow(
        listOf(summary("a"), summary("b", unreadCount = 2), summary("c", isMuted = true)),
    )
    private val archived = MutableStateFlow(listOf(summary("z", isArchived = true)))
    private val repository = mockk<ConversationRepository> {
        every { observeConversations() } returns conversations
        every { observeArchived() } returns archived
    }
    private val preferences = mockk<ConversationPreferencesRepository>(relaxed = true) {
        coEvery { setMuted(any(), any()) } returns PreferenceResult.Success
        coEvery { markRead(any()) } returns PreferenceResult.Success
        coEvery { markUnread(any()) } returns PreferenceResult.Success
        coEvery { setArchived(any(), any()) } returns PreferenceResult.Success
    }
    private val viewModel by lazy { ConversationSelectionViewModel(repository, preferences) }

    @Test
    fun `toggling selects and unselects a conversation`() = runTest {
        viewModel.uiState.test {
            assertFalse(awaitItem().isActive)
            viewModel.onToggled("a")
            assertEquals(setOf("a"), awaitItem().selectedIds)
            viewModel.onToggled("b")
            assertEquals(setOf("a", "b"), awaitItem().selectedIds)
            viewModel.onToggled("a")
            assertEquals(setOf("b"), awaitItem().selectedIds)
            viewModel.onSelectionClosed()
            assertFalse(awaitItem().isActive)
        }
    }

    @Test
    fun `select all takes what the list shows`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectAll(listOf("a", "c"))
            assertEquals(setOf("a", "c"), awaitItem().selectedIds)
        }
    }

    @Test
    fun `a conversation that leaves the list leaves the selection`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectAll(listOf("a", "b"))
            assertEquals(setOf("a", "b"), awaitItem().selectedIds)
            conversations.value = conversations.value.filterNot { it.id == "a" }
            assertEquals(setOf("b"), awaitItem().selectedIds)
        }
    }

    @Test
    fun `muting applies to the whole selection and ends the selection`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectAll(listOf("a", "b"))
            awaitItem()

            viewModel.onMuteClicked()

            assertFalse(awaitItem().isActive)
            coVerify { preferences.setMuted(setOf("a", "b"), true) }
        }
    }

    @Test
    fun `a muted selection is unmuted`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onToggled("c")
            awaitItem()

            viewModel.onMuteClicked()
            awaitItem()

            coVerify { preferences.setMuted(setOf("c"), false) }
        }
    }

    @Test
    fun `pinning applies to the whole selection`() = runTest {
        coEvery { preferences.setPinned(any(), any()) } returns PreferenceResult.Success

        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectAll(listOf("a", "b"))
            awaitItem()

            viewModel.onPinClicked()
            awaitItem()

            coVerify { preferences.setPinned(setOf("a", "b"), true) }
        }
    }

    @Test
    fun `archiving offers Undo, and Undo brings the conversations back`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectAll(listOf("a", "b"))
            awaitItem()

            viewModel.onArchiveClicked()
            var state = awaitItem()
            while (state.undoArchive.isEmpty()) state = awaitItem()
            assertEquals(setOf("a", "b"), state.undoArchive)
            coVerify { preferences.setArchived(setOf("a", "b"), true) }

            viewModel.onUndoArchiveHandled(true)
            assertTrue(awaitItem().undoArchive.isEmpty())
            coVerify { preferences.setArchived(setOf("a", "b"), false) }
        }
    }

    @Test
    fun `an archived conversation can be selected and unarchived`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onToggled("z")
            assertEquals(ArchiveAction.UNARCHIVE, awaitItem().actions.archive)

            viewModel.onArchiveClicked()
            awaitItem()

            coVerify { preferences.setArchived(setOf("z"), false) }
        }
    }

    @Test
    fun `deleting asks first, then deletes the selection and offers Undo`() = runTest {
        coEvery { preferences.deleteForMe(any()) } returns PreferenceResult.Success
        coEvery { preferences.undoDeleteForMe(any()) } returns PreferenceResult.Success

        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectAll(listOf("a", "b"))
            awaitItem()

            viewModel.deletion.onRequested(setOf("a", "b"))
            assertEquals(setOf("a", "b"), awaitItem().deletion.pending)
            coVerify(exactly = 0) { preferences.deleteForMe(any()) }

            viewModel.deletion.onConfirmed()
            var state = awaitItem()
            while (state.deletion.undo.isEmpty()) state = awaitItem()
            assertEquals(setOf("a", "b"), state.deletion.undo)
            coVerify { preferences.deleteForMe(setOf("a", "b")) }

            viewModel.deletion.onUndoHandled(true)
            awaitItem()
            coVerify { preferences.undoDeleteForMe(setOf("a", "b")) }
        }
    }

    @Test
    fun `cancelling the dialog deletes nothing`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.deletion.onRequested(setOf("a"))
            awaitItem()

            viewModel.deletion.onDismissed()

            assertTrue(awaitItem().deletion.pending.isEmpty())
            coVerify(exactly = 0) { preferences.deleteForMe(any()) }
        }
    }

    @Test
    fun `a refused delete is reported`() = runTest {
        coEvery { preferences.deleteForMe(any()) } returns PreferenceResult.Failure(PreferenceError.UNKNOWN)

        viewModel.uiState.test {
            awaitItem()
            viewModel.deletion.onRequested(setOf("a"))
            awaitItem()
            viewModel.deletion.onConfirmed()
            var state = awaitItem()
            while (state.error == null) state = awaitItem()
            assertEquals(PreferenceError.UNKNOWN, state.error)
        }
    }

    @Test
    fun `read and unread follow whether anything selected is unread`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectAll(listOf("a", "b"))
            awaitItem()
            viewModel.onReadClicked()
            awaitItem()
            coVerify { preferences.markRead(setOf("a", "b")) }

            viewModel.onToggled("a")
            awaitItem()
            viewModel.onReadClicked()
            awaitItem()
            coVerify { preferences.markUnread(setOf("a")) }
        }
    }

    @Test
    fun `a refused change is reported and can be dismissed`() = runTest {
        val refused = PreferenceResult.Failure(PreferenceError.NETWORK_UNAVAILABLE)
        coEvery { preferences.setMuted(any(), any()) } returns refused

        viewModel.uiState.test {
            awaitItem()
            viewModel.onToggled("a")
            awaitItem()
            viewModel.onMuteClicked()
            var state = awaitItem()
            while (state.error == null) state = awaitItem()
            assertEquals(PreferenceError.NETWORK_UNAVAILABLE, state.error)

            viewModel.onErrorShown()
            assertNull(awaitItem().error)
        }
    }

    private fun summary(id: String, unreadCount: Int = 0, isMuted: Boolean = false, isArchived: Boolean = false) =
        ConversationSummary(
            id = id,
            type = ConversationType.DIRECT,
            title = id,
            lastMessagePreview = null,
            lastActivityAtEpochMillis = 1L,
            unreadCount = unreadCount,
            canPost = true,
            isMuted = isMuted,
            isArchived = isArchived,
        )
}
