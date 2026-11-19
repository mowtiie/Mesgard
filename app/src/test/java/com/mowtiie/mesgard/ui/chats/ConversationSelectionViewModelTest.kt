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
import org.junit.Rule
import org.junit.Test

class ConversationSelectionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversations = MutableStateFlow(
        listOf(summary("a"), summary("b", unreadCount = 2), summary("c", isMuted = true)),
    )
    private val repository = mockk<ConversationRepository> { every { observeConversations() } returns conversations }
    private val preferences = mockk<ConversationPreferencesRepository>(relaxed = true) {
        coEvery { setMuted(any(), any()) } returns PreferenceResult.Success
        coEvery { markRead(any()) } returns PreferenceResult.Success
        coEvery { markUnread(any()) } returns PreferenceResult.Success
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

    private fun summary(id: String, unreadCount: Int = 0, isMuted: Boolean = false) = ConversationSummary(
        id = id,
        type = ConversationType.DIRECT,
        title = id,
        lastMessagePreview = null,
        lastActivityAtEpochMillis = 1L,
        unreadCount = unreadCount,
        canPost = true,
        isMuted = isMuted,
    )
}
