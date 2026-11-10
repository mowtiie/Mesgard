package com.mowtiie.mesgard.ui.chats

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConversationListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversations = MutableStateFlow(emptyList<ConversationSummary>())
    private val repository = mockk<ConversationRepository> {
        every { observeConversations() } returns conversations
        coEvery { refresh() } returns RefreshResult.Success
    }

    @Test
    fun `refreshes the conversations when it is created`() = runTest {
        ConversationListViewModel(repository).uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { repository.refresh() }
    }

    @Test
    fun `exposes the stored conversations`() = runTest {
        conversations.value = listOf(summary("chat"))

        ConversationListViewModel(repository).uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("chat"), state.conversations.map { it.id })
            assertTrue(state.isLoaded)
            assertFalse(state.showEmptyMessage)
        }
    }

    @Test
    fun `shows the empty message once loaded without conversations`() = runTest {
        ConversationListViewModel(repository).uiState.test {
            assertTrue(expectMostRecentItem().showEmptyMessage)
        }
    }

    @Test
    fun `reports a refresh failure without dropping stored conversations`() = runTest {
        conversations.value = listOf(summary("chat"))
        coEvery { repository.refresh() } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        ConversationListViewModel(repository).uiState.test {
            val state = expectMostRecentItem()
            assertEquals(RefreshError.NETWORK_UNAVAILABLE, state.refreshError)
            assertEquals(1, state.conversations.size)
        }
    }

    @Test
    fun `reports refreshing while a refresh is in progress`() = runTest {
        val pending = CompletableDeferred<RefreshResult>()
        coEvery { repository.refresh() } coAnswers { pending.await() }

        ConversationListViewModel(repository).uiState.test {
            assertTrue(expectMostRecentItem().isRefreshing)
            pending.complete(RefreshResult.Success)
            val finished = awaitItem()
            assertFalse(finished.isRefreshing)
            assertNull(finished.refreshError)
        }
    }

    @Test
    fun `ignores a refresh request while a refresh is in progress`() = runTest {
        val pending = CompletableDeferred<RefreshResult>()
        coEvery { repository.refresh() } coAnswers { pending.await() }
        val viewModel = ConversationListViewModel(repository)

        viewModel.onRefresh()
        pending.complete(RefreshResult.Success)

        coVerify(exactly = 1) { repository.refresh() }
    }

    private fun summary(id: String) = ConversationSummary(
        id = id,
        type = ConversationType.DIRECT,
        title = "Title",
        lastMessagePreview = "Hello",
        lastActivityAtEpochMillis = 1_000L,
        unreadCount = 0,
    )
}
