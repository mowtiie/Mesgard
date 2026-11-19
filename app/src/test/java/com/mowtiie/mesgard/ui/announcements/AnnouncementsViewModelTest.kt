package com.mowtiie.mesgard.ui.announcements

import app.cash.turbine.test
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.domain.model.RefreshError
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.repository.ConversationRepository
import com.mowtiie.mesgard.domain.repository.MessageRepository
import com.mowtiie.mesgard.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnnouncementsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversation = MutableStateFlow<ConversationSummary?>(null)
    private val messages = MutableStateFlow(emptyList<Message>())
    private val conversationRepository = mockk<ConversationRepository> {
        every { observeAnnouncements() } returns conversation
        coEvery { refresh() } returns RefreshResult.Success
    }
    private val messageRepository = mockk<MessageRepository> {
        every { observeMessages("news", any()) } returns messages
        coEvery { refresh("news") } returns RefreshResult.Success
        coEvery { markConversationRead("news") } returns RefreshResult.Success
    }
    private val viewModel by lazy { AnnouncementsViewModel(conversationRepository, messageRepository) }

    @Test
    fun `shows the empty message when the organization has not announced anything`() = runTest {
        conversation.value = summary()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.showEmptyMessage)
            assertEquals("news", state.conversationId)
        }
    }

    @Test
    fun `lists the announcements and leaves out deleted ones`() = runTest {
        conversation.value = summary(canPost = true)
        messages.value = listOf(message("m2"), message("m1", isDeleted = true))

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("m2"), state.announcements.map { it.id })
            assertTrue(state.canPost)
            assertFalse(state.showEmptyMessage)
        }
    }

    @Test
    fun `a member who cannot post gets no compose action`() = runTest {
        conversation.value = summary(canPost = false)

        viewModel.uiState.test {
            assertFalse(expectMostRecentItem().canPost)
        }
    }

    @Test
    fun `unread announcements are marked read only while the tab is visible`() = runTest {
        conversation.value = summary(unreadCount = 2)

        viewModel.uiState.test {
            expectMostRecentItem()
            coVerify(exactly = 0) { messageRepository.markConversationRead("news") }

            viewModel.onVisibilityChanged(true)

            coVerify(exactly = 1) { messageRepository.markConversationRead("news") }
        }
    }

    @Test
    fun `a failed refresh is reported once and can be dismissed`() = runTest {
        coEvery { conversationRepository.refresh() } returns RefreshResult.Failure(RefreshError.NETWORK_UNAVAILABLE)

        viewModel.uiState.test {
            assertEquals(RefreshError.NETWORK_UNAVAILABLE, expectMostRecentItem().refreshError)
            viewModel.onRefreshErrorShown()
            assertEquals(null, expectMostRecentItem().refreshError)
        }
    }

    private fun summary(canPost: Boolean = false, unreadCount: Int = 0) = ConversationSummary(
        id = "news",
        type = ConversationType.ANNOUNCEMENT,
        title = null,
        lastMessagePreview = null,
        lastActivityAtEpochMillis = 1_000L,
        unreadCount = unreadCount,
        canPost = canPost,
    )

    private fun message(id: String, isDeleted: Boolean = false) = Message(
        id = id,
        conversationId = "news",
        senderId = "admin",
        senderName = "Admin Cruz",
        body = "Classes are suspended",
        createdAtEpochMillis = 1_000L,
        isEdited = false,
        isDeleted = isDeleted,
        status = MessageStatus.SENT,
        isOwn = false,
    )
}
